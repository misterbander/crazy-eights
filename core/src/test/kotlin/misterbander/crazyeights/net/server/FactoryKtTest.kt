package misterbander.crazyeights.net.server

import misterbander.crazyeights.net.server.ServerCard.Rank
import misterbander.crazyeights.net.server.ServerCard.Suit
import kotlin.test.Test
import kotlin.test.assertEquals

class FactoryKtTest
{
	@Test
	fun `String_toCard() converts to the correct ServerCard`()
	{
		assertEquals(expected = ServerCard(rank = Rank.ACE, suit = Suit.DIAMONDS), actual = "A♢".toCard())
		assertEquals(expected = ServerCard(rank = Rank.SIX, suit = Suit.DIAMONDS), actual = "6♢".toCard())
		assertEquals(expected = ServerCard(rank = Rank.EIGHT, suit = Suit.CLUBS), actual = "8♣".toCard())
		assertEquals(expected = ServerCard(rank = Rank.TEN, suit = Suit.HEARTS), actual = "10♡".toCard())
		assertEquals(expected = ServerCard(rank = Rank.JACK, suit = Suit.SPADES), actual = "J♠".toCard())
		assertEquals(expected = ServerCard(rank = Rank.QUEEN, suit = Suit.SPADES), actual = "Q♠".toCard())
		assertEquals(expected = ServerCard(rank = Rank.KING, suit = Suit.SPADES), actual = "K♠".toCard())
	}
}
