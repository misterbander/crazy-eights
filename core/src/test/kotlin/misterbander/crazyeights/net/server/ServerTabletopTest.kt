package misterbander.crazyeights.net.server

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.esotericsoftware.kryonet.Connection
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import ktx.app.KtxApplicationAdapter
import ktx.collections.*
import misterbander.crazyeights.kryo.objectMoveEventPool
import misterbander.crazyeights.kryo.objectRotateEventPool
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupCreateEvent
import misterbander.crazyeights.net.packets.CardGroupDetachEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.packets.ObjectDisownEvent
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.net.server.ServerCard.Rank
import misterbander.crazyeights.net.server.ServerCard.Suit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerTabletopTest
{
	@MockK
	private lateinit var server: CrazyEightsServer
	@MockK
	private lateinit var aliceConnection: Connection
	@MockK
	private lateinit var bobConnection: Connection
	
	@BeforeTest
	fun setup()
	{
		HeadlessApplication(object : KtxApplicationAdapter
		{})
		MockKAnnotations.init(this)
		every { server.server.sendToAllTCP(any()) } just runs
		every { server.server.sendToAllExceptTCP(any(), any()) } just runs
		every { aliceConnection.id } returns 0
		every { aliceConnection.arbitraryData } returns User("alice")
		every { aliceConnection.arbitraryData = any() } just runs
		every { aliceConnection.sendTCP(any()) } returns 0
		every { bobConnection.id } returns 1
		every { bobConnection.arbitraryData } returns User("bob")
		every { bobConnection.arbitraryData = any() } just runs
		every { bobConnection.sendTCP(any()) } returns 0
	}
	
	private fun newDefaultServerTabletop(): ServerTabletop
	{
		var id = 0
		every { server.newId() } answers { id++ }
		return newServerTabletop(
			server,
			playerHands = mapOf(User("alice") to emptyArray(), User("bob") to emptyArray())
		) { id++ }
	}
	
	@Test
	fun `onUserJoined() adds a user`()
	{
		var id = 0
		val tabletop = newServerTabletop(server) { id++ }
		
		tabletop.onUserJoined(aliceConnection, User("alice"))
		
		assertTrue { "alice" in tabletop.users }
		assertEquals(expected = 1, tabletop.userCount)
		assertContentEquals(expected = GdxArray(), actual = tabletop.hands["alice"])
	}
	
	@Test
	fun `removeUser() on a human user with empty hand removes them`()
	{
		val tabletop = newDefaultServerTabletop()
		
		tabletop.removeUser(User("alice"))
		
		assertTrue { "alice" !in tabletop.users }
		assertTrue { "bob" in tabletop.users }
		assertEquals(expected = 1, actual = tabletop.userCount)
		assertNull(tabletop.hands["alice"])
	}
	
	@Test
	fun `removeUser() on a human user with a non-empty hand removes them but keeps the cards in their hands`()
	{
		var id = 0
		val tabletop = newServerTabletop(
			server,
			playerHands = mapOf(
				User("alice") to arrayOf("J♡", "7♣"),
				User("bob") to arrayOf("9♢", "3♢", "4♡", "2♠", "7♡")
			)
		) { id++ }
		
		tabletop.removeUser(User("alice"))
		
		assertTrue { "alice" !in tabletop.users }
		assertTrue { "bob" in tabletop.users }
		assertEquals(expected = 1, actual = tabletop.userCount)
		assertContentEquals(
			expected = gdxArrayOf(
				ServerCard(id = 0, rank = Rank.JACK, suit = Suit.HEARTS),
				ServerCard(id = 1, rank = Rank.SEVEN, suit = Suit.CLUBS)
			),
			actual = tabletop.hands["alice"]
		)
	}
	
	@Test
	fun `removeUser() on an AI user removes them and their hands`()
	{
		var id = 0
		val tabletop = newServerTabletop(
			server,
			playerHands = mapOf(
				User("alice", isAi = true) to arrayOf("J♡", "7♣"),
				User("bob") to arrayOf("9♢", "3♢", "4♡", "2♠", "7♡")
			)
		) { id++ }
		val card1 = tabletop.idToObjectMap[0]!!
		val card2 = tabletop.idToObjectMap[1]!!
		
		tabletop.removeUser(User("alice", isAi = true))
		
		assertTrue { "alice" !in tabletop.users }
		assertTrue { "bob" in tabletop.users }
		assertEquals(expected = 1, actual = tabletop.userCount)
		assertNull(tabletop.hands["alice"])
		assertTrue { card1 in tabletop.serverObjects }
		assertTrue { card2 in tabletop.serverObjects }
	}
	
	@Test
	fun `onObjectLock() locks an unlocked card`()
	{
		val tabletop = newDefaultServerTabletop()
		val card = ServerCard(server.newId())
		tabletop.addServerObject(card)
		
		tabletop.onObjectLock(ObjectLockEvent(4, "alice"))
		
		assertEquals(expected = ServerCard(4, lockHolder = "alice"), actual = card)
		assertTrue { card.isLocked }
		
		tabletop.onObjectLock(ObjectLockEvent(4, "bob"))
		
		assertEquals(expected = ServerCard(4, lockHolder = "alice"), actual = card)
		assertTrue { card.isLocked }
	}
	
	@Test
	fun `onObjectUnlock() by the correct lock holder unlocks a card`()
	{
		val tabletop = newDefaultServerTabletop()
		val card = ServerCard(server.newId(), lockHolder = "alice")
		val card2 = ServerCard(server.newId(), lockHolder = "bob")
		tabletop.addServerObject(card)
		tabletop.addServerObject(card2)
		
		tabletop.onObjectUnlock(ObjectUnlockEvent(4, "bob", false))
		
		assertEquals(expected = ServerCard(4, lockHolder = "alice"), actual = card)
		assertTrue { card.isLocked }
		
		tabletop.onObjectUnlock(ObjectUnlockEvent(4, "alice", false))
		
		assertEquals(expected = ServerCard(4), actual = card)
		assertTrue { !card.isLocked }
		
		tabletop.onObjectUnlock(ObjectUnlockEvent(5, "bob"))
		
		assertEquals(expected = ServerCard(5, isFaceUp = true), actual = card2)
		assertTrue { !card2.isLocked }
	}
	
	@Test
	fun `onObjectOwn() owns a card`()
	{
		var id = 0
		val tabletop = newServerTabletop(server, playerHands = mapOf(User("alice") to emptyArray())) { id++ }
		val card = ServerCard(id++)
		tabletop.addServerObject(card)
		
		tabletop.onObjectOwn(aliceConnection, ObjectOwnEvent(4, "alice"))
		
		assertTrue { card !in tabletop.serverObjects }
		assertTrue { card in tabletop.hands["alice"] }
	}
	
	@Test
	fun `onObjectDisown() disowns a card`()
	{
		var id = 0
		val tabletop = newServerTabletop(server, playerHands = mapOf(User("alice") to arrayOf("J♡"))) { id++ }
		val card = tabletop.idToObjectMap[0] as ServerCard
		
		tabletop.onObjectDisown(
			aliceConnection,
			ObjectDisownEvent(id = 0, x = 17F, y = 34F, rotation = 50F, isFaceUp = true, disownerUsername = "alice")
		)
		
		assertEquals(
			expected = ServerCard(
				id = 0,
				x = 17F,
				y = 34F,
				rotation = 50F,
				rank = Rank.JACK,
				suit = Suit.HEARTS,
				isFaceUp = true,
				lockHolder = "alice"
			),
			actual = card
		)
		assertTrue { card in tabletop.serverObjects }
		assertTrue { card !in tabletop.hands["alice"] }
	}
	
	@Test
	fun `onObjectMove() by lock holder moves a card`()
	{
		val tabletop = newDefaultServerTabletop()
		val card = ServerCard(server.newId(), lockHolder = "alice")
		tabletop.addServerObject(card)
		
		tabletop.onObjectMove(aliceConnection, objectMoveEventPool.obtain().apply {
			id = 4
			x = 17F
			y = 34F
		})
		
		assertEquals(
			expected = ServerCard(id = 4, x = 17F, y = 34F, justMoved = true, lockHolder = "alice"),
			actual = card
		)
		
		tabletop.onObjectMove(bobConnection, objectMoveEventPool.obtain().apply {
			id = 4
			x = 51F
			y = 68F
		})
		
		assertEquals(
			expected = ServerCard(id = 4, x = 17F, y = 34F, justMoved = true, lockHolder = "alice"),
			actual = card
		)
	}
	
	@Test
	fun `onObjectRotate() by lock holder rotates a card`()
	{
		val tabletop = newDefaultServerTabletop()
		val card = ServerCard(server.newId(), lockHolder = "alice")
		tabletop.addServerObject(card)
		
		tabletop.onObjectRotate(aliceConnection, objectRotateEventPool.obtain().apply {
			id = 4
			rotation = -69F
		})
		
		assertEquals(
			expected = ServerCard(id = 4, rotation = -69F, lockHolder = "alice", justRotated = true),
			actual = card
		)
		
		tabletop.onObjectRotate(bobConnection, objectRotateEventPool.obtain().apply {
			id = 4
			rotation = 20F
		})
		
		assertEquals(
			expected = ServerCard(id = 4, rotation = -69F, lockHolder = "alice", justRotated = true),
			actual = card
		)
	}
	
	@Test
	fun `onCardGroupCreate() creates a new card group`()
	{
		val tabletop = newDefaultServerTabletop()
		val card = ServerCard(server.newId(), x = -12F, y = 36F, rotation = 80F)
		val card2 = ServerCard(server.newId(), x = 0F, y = 450F, rotation = -999F)
		tabletop.addServerObject(card)
		tabletop.addServerObject(card2)
		
		tabletop.onCardGroupCreate(
			CardGroupCreateEvent(
				cards = gdxArrayOf(
					ServerCard(id = 4, x = 17F, y = -34F, rotation = 20F),
					ServerCard(id = 5, x = 1F, y = 451F, rotation = -800F)
				)
			)
		)
		
		val newCardGroup = tabletop.idToObjectMap[6] as ServerCardGroup
		assertEquals(
			expected = ServerCardGroup(
				id = 6,
				x = 17F,
				y = -34F,
				rotation = 20F,
				cards = gdxArrayOf(
					ServerCard(id = 4, x = -0F, y = 0F, rotation = 0F),
					ServerCard(id = 5, x = -1F, y = 1F, rotation = -900F)
				)
			),
			actual = newCardGroup
		)
		assertTrue { newCardGroup in tabletop.serverObjects }
		assertTrue { card !in tabletop.serverObjects }
		assertTrue { card2 !in tabletop.serverObjects }
	}
	
	@Test
	fun `onCardGroupChange() changes the card group ids of cards`()
	{
		val tabletop = newDefaultServerTabletop()
		val cardGroup = ServerCardGroup(
			server.newId(),
			x = 10F,
			y = 20F,
			rotation = 30F,
			cards = gdxArrayOf(
				ServerCard(server.newId()),
				ServerCard(server.newId(), x = -1F, y = 1F)
			)
		)
		val cardGroup2 = ServerCardGroup(
			server.newId(),
			x = 11F,
			y = 21F,
			rotation = -31F,
			cards = gdxArrayOf(
				ServerCard(server.newId()),
				ServerCard(server.newId(), x = -1F, y = 1F)
			)
		)
		val card = ServerCard(server.newId(), x = -12F, y = 36F, rotation = 80F)
		val card2 = ServerCard(server.newId(), x = 0F, y = 450F, rotation = -999F)
		tabletop.addServerObject(cardGroup)
		tabletop.addServerObject(cardGroup2)
		tabletop.addServerObject(card)
		tabletop.addServerObject(card2)
		
		tabletop.onCardGroupChange(CardGroupChangeEvent(gdxArrayOf(card, card2), 4, "alice"))
		
		assertEquals(
			expected = ServerCardGroup(
				id = 4,
				x = 10F,
				y = 20F,
				rotation = 30F,
				cards = gdxArrayOf(
					ServerCard(id = 5),
					ServerCard(id = 6, x = -1F, y = 1F),
					ServerCard(id = 10, x = -2F, y = 2F),
					ServerCard(id = 11, x = -3F, y = 3F, rotation = -1080F)
				)
			),
			actual = cardGroup
		)
		assertTrue { card !in tabletop.serverObjects }
		assertTrue { card2 !in tabletop.serverObjects }
		
		tabletop.onCardGroupChange(CardGroupChangeEvent(GdxArray(cardGroup.cards), 7, "alice"))
		
		assertEquals(
			expected = ServerCardGroup(
				id = 7,
				x = 11F,
				y = 21F,
				rotation = -31F,
				cards = gdxArrayOf(
					ServerCard(id = 8),
					ServerCard(id = 9, x = -1F, y = 1F),
					ServerCard(id = 5, x = -2F, y = 2F),
					ServerCard(id = 6, x = -3F, y = 3F),
					ServerCard(id = 10, x = -4F, y = 4F),
					ServerCard(id = 11, x = -5F, y = 5F, rotation = -1080F)
				)
			),
			actual = cardGroup2
		)
	}
	
	@Test
	fun `onCardGroupDetach() detaches a card group from its card holder`()
	{
		var id = 0
		every { server.newId() } answers { id++ }
		val tabletop = newServerTabletop(
			server,
			drawStack = arrayOf("J♡", "7♣"),
			discardPile = arrayOf("9♢", "3♢", "4♡", "2♠", "7♡")
		) { id++ }
		tabletop.drawStackHolder.apply {
			x = 17F
			y = 34F
			rotation = -20F
		}
		
		tabletop.onCardGroupDetach(CardGroupDetachEvent(0, changerUsername = "alice"))
		
		assertEquals(
			expected = ServerCardHolder(id = 0, x = 17F, y = 34F, rotation = -20F, cardGroup = ServerCardGroup(11)),
			actual = tabletop.drawStackHolder
		)
	}
	
	@Test
	fun `onCardGroupDismantle() dismantles a card group`()
	{
		val tabletop = newDefaultServerTabletop()
		val cardGroup = ServerCardGroup(
			server.newId(),
			x = 10F,
			y = 20F,
			rotation = 30F,
			cards = gdxArrayOf(
				ServerCard(server.newId()),
				ServerCard(server.newId(), x = -1F, y = 1F)
			)
		)
		tabletop.addServerObject(cardGroup)
		
		tabletop.onCardGroupDismantle(aliceConnection, CardGroupDismantleEvent(4))
		
		val card = tabletop.idToObjectMap[5] as ServerCard
		val card2 = tabletop.idToObjectMap[6] as ServerCard
		
		assertEquals(expected = ServerCard(id = 5, x = 10F, y = 20F, rotation = 30F), actual = card)
		assertEquals(expected = ServerCard(id = 6, x = 9F, y = 21F, rotation = 30F), actual = card2)
		assertTrue { cardGroup !in tabletop.serverObjects }
		assertTrue { card in tabletop.serverObjects }
		assertTrue { card2 in tabletop.serverObjects }
	}
	
	@Test
	fun `createGameState() creates the correct ServerGameState from its current state`()
	{
		var id = 0
		val tabletop = newServerTabletop(
			server,
			playerHands = mapOf(
				User("alice") to arrayOf("7♠", "K♢", "3♠", "K♡", "3♡"),
				User("bob") to arrayOf("9♡", "Q♢", "5♢", "10♡", "J♠", "3♣")
			),
			drawStack = arrayOf("J♡", "7♣"),
			discardPile = arrayOf("9♢", "3♢", "4♡", "2♠", "7♡")
		) { id++ }
		tabletop.serverGameState = tabletop.createGameState()
		
		assertContentEquals(
			expected = tabletop.drawStackHolder.cardGroup.cards,
			actual = tabletop.serverGameState!!.drawStack
		)
		assertContentEquals(
			expected = tabletop.discardPileHolder.cardGroup.cards,
			actual = tabletop.serverGameState!!.discardPile
		)
		assertContentEquals(
			expected = tabletop.hands["alice"],
			actual = tabletop.serverGameState!!.playerHands[User("alice")]
		)
		assertContentEquals(
			expected = tabletop.hands["bob"],
			actual = tabletop.serverGameState!!.playerHands[User("bob")]
		)
	}
}
