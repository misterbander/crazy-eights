package misterbander.crazyeights.net.server

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.esotericsoftware.kryonet.Connection
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import ktx.app.KtxApplicationAdapter
import ktx.async.KtxAsync
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
	fun `onObjectOwn() owns an unowned card`()
	{
		var id = 0
		val tabletop = newServerTabletop(
			server,
			playerHands = mapOf(User("alice") to emptyArray(), User("bob") to emptyArray())
		) { id++ }
		val card = ServerCard(id++)
		tabletop.addServerObject(card)
		
		tabletop.onObjectOwn(aliceConnection, ObjectOwnEvent(4, "alice"))
		
		assertEquals(expected = "alice", actual = card.getOwner(tabletop))
		assertTrue { card !in tabletop.serverObjects }
		
		tabletop.onObjectOwn(bobConnection, ObjectOwnEvent(4, "bob"))
		
		assertEquals(expected = "alice", actual = card.getOwner(tabletop))
		assertTrue { card !in tabletop.serverObjects }
	}
	
	@Test
	fun `onObjectDisown() by the owner disowns a card`()
	{
		var id = 0
		val tabletop = newServerTabletop(
			server,
			playerHands = mapOf(User("alice") to arrayOf("J♡"), User("bob") to emptyArray())
		) { id++ }
		val card = tabletop.findObjectById<ServerCard>(0)
		
		tabletop.onObjectDisown(
			bobConnection,
			ObjectDisownEvent(id = 4, x = 17F, y = 34F, rotation = 50F, isFaceUp = true, disownerUsername = "bob")
		)
		
		assertEquals(expected = ServerCard(0, rank = Rank.JACK, suit = Suit.HEARTS), actual = card)
		assertEquals(expected = "alice", actual = card.getOwner(tabletop))
		assertTrue { card !in tabletop.serverObjects }
		
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
				lockHolder = "alice",
				lastOwner = "alice"
			),
			actual = card
		)
		assertEquals(expected = null, actual = card.getOwner(tabletop))
		assertTrue { card in tabletop.serverObjects }
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
		
		val newCardGroup = tabletop.findObjectById<ServerCardGroup>(6)
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
		
		val card = tabletop.findObjectById<ServerCard>(5)
		val card2 = tabletop.findObjectById<ServerCard>(6)
		
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
		val gameState = tabletop.createGameState()
		tabletop.serverGameState = gameState
		
		assertContentEquals(
			expected = tabletop.drawStackHolder.cardGroup.cards,
			actual = gameState.drawStack
		)
		assertContentEquals(
			expected = tabletop.discardPileHolder.cardGroup.cards,
			actual = gameState.discardPile
		)
		assertContentEquals(
			expected = tabletop.hands["alice"],
			actual = gameState.playerHands[User("alice")]
		)
		assertContentEquals(
			expected = tabletop.hands["bob"],
			actual = gameState.playerHands[User("bob")]
		)
	}
	
	@Test
	fun `The tabletop state should match its game state after clicking on the draw stack`()
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
		val drawStack = tabletop.drawStackHolder.cardGroup.cards
		val gameState = tabletop.createGameState()
		tabletop.serverGameState = gameState
		
		val drawStackTopCardId = drawStack.peek().id
		tabletop.onObjectLock(ObjectLockEvent(drawStackTopCardId, "alice"))
		tabletop.onObjectUnlock(ObjectUnlockEvent(drawStackTopCardId, "alice"))
		
		assertEquals(expected = 1, actual = drawStack.size)
		assertEquals(expected = gdxArrayOf<ServerObject>(
			ServerCard(0, rank = Rank.SEVEN, suit = Suit.SPADES),
			ServerCard(1, rank = Rank.KING, suit = Suit.DIAMONDS),
			ServerCard(2, rank = Rank.THREE, suit = Suit.SPADES),
			ServerCard(3, rank = Rank.KING, suit = Suit.HEARTS),
			ServerCard(4, rank = Rank.THREE, suit = Suit.HEARTS),
			ServerCard(14, x = -1F, y = 1F, rank = Rank.SEVEN, suit = Suit.CLUBS, isFaceUp = true),
		), actual = tabletop.hands["alice"])
		assertContentEquals(expected = tabletop.hands["alice"], actual = gameState.playerHands[User("alice")])
		assertContentEquals(expected = tabletop.hands["bob"], actual = gameState.playerHands[User("bob")])
		assertContentEquals(expected = tabletop.drawStackHolder.cardGroup.cards, actual = gameState.drawStack)
		assertContentEquals(expected = tabletop.discardPileHolder.cardGroup.cards, actual = gameState.discardPile)
	}
	
	@Test
	fun `The tabletop state should match its game state after clicking on the draw stack when only one card is in it`()
	{
		var id = 0
		val tabletop = newServerTabletop(
			server,
			playerHands = mapOf(
				User("alice") to arrayOf("7♠", "K♢", "3♠", "K♡", "3♡"),
				User("bob") to arrayOf("9♡", "Q♢", "5♢", "10♡", "J♠", "3♣")
			),
			drawStack = arrayOf("J♡"),
			discardPile = arrayOf("9♢", "3♢", "4♡", "2♠", "7♡")
		) { id++ }
		val drawStack = tabletop.drawStackHolder.cardGroup.cards
		val gameState = tabletop.createGameState()
		tabletop.serverGameState = gameState
		
		val drawStackTopCardId = drawStack.peek().id
		tabletop.onObjectLock(ObjectLockEvent(drawStackTopCardId, "alice"))
		tabletop.onObjectUnlock(ObjectUnlockEvent(drawStackTopCardId, "alice"))
		
		assertEquals(expected = 4, actual = drawStack.size)
		assertEquals(expected = gdxArrayOf<ServerObject>(
			ServerCard(0, rank = Rank.SEVEN, suit = Suit.SPADES),
			ServerCard(1, rank = Rank.KING, suit = Suit.DIAMONDS),
			ServerCard(2, rank = Rank.THREE, suit = Suit.SPADES),
			ServerCard(3, rank = Rank.KING, suit = Suit.HEARTS),
			ServerCard(4, rank = Rank.THREE, suit = Suit.HEARTS),
			ServerCard(13, rank = Rank.JACK, suit = Suit.HEARTS, isFaceUp = true),
		), actual = tabletop.hands["alice"])
		assertContentEquals(expected = tabletop.hands["alice"], actual = gameState.playerHands[User("alice")])
		assertContentEquals(expected = tabletop.hands["bob"], actual = gameState.playerHands[User("bob")])
		assertContentEquals(expected = tabletop.drawStackHolder.cardGroup.cards, actual = gameState.drawStack)
		assertContentEquals(expected = tabletop.discardPileHolder.cardGroup.cards, actual = gameState.discardPile)
	}
	
	@Test
	fun `The tabletop state after acceptDrawTwoPenalty() should match its game state when amount of cards to draw equals the number of cards in the draw stack`()
	{
		KtxAsync.initiate()
		var id = 0
		val tabletop = newServerTabletop(
			server,
			playerHands = mapOf(
				User("alice") to arrayOf("7♠", "K♢", "3♠", "K♡", "3♡"),
				User("bob") to arrayOf("9♡", "Q♢", "5♢", "10♡", "J♠", "3♣")
			),
			drawStack = arrayOf("J♡", "A♣"),
			discardPile = arrayOf("9♢", "3♢")
		) { id++ }
		val drawStack = tabletop.drawStackHolder.cardGroup.cards
		val gameState = tabletop.createGameState(drawTwoEffectCardCount = 4)
		tabletop.serverGameState = gameState
		
		val drawStackTopCardId = drawStack.peek().id
		tabletop.onObjectLock(ObjectLockEvent(drawStackTopCardId, "alice"))
		
		runBlocking {
			delay(100L)
			tabletop.onActionLockReleaseEvent(aliceConnection)
			tabletop.onActionLockReleaseEvent(bobConnection)
			delay(100L)
			tabletop.onActionLockReleaseEvent(aliceConnection)
			tabletop.onActionLockReleaseEvent(bobConnection)
			
			assertTrue { drawStack.isEmpty }
			assertEquals(expected = 8, actual = tabletop.hands["alice"]!!.size)
			assertContentEquals(expected = tabletop.hands["alice"], actual = gameState.playerHands[User("alice")])
			assertContentEquals(expected = tabletop.hands["bob"], actual = gameState.playerHands[User("bob")])
			assertContentEquals(expected = tabletop.drawStackHolder.cardGroup.cards, actual = gameState.drawStack)
			assertContentEquals(expected = tabletop.discardPileHolder.cardGroup.cards, actual = gameState.discardPile)
		}
	}
}
