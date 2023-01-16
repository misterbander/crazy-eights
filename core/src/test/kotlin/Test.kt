import misterbander.crazyeights.net.server.ServerCard
import misterbander.crazyeights.net.server.ServerCard.Rank
import misterbander.crazyeights.net.server.ServerCard.Suit
import misterbander.crazyeights.net.server.game.Ruleset
import misterbander.crazyeights.net.server.game.ai.OracleAgent
import misterbander.crazyeights.net.server.game.toCard
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class Test
{
	@Test
	fun `Test String_toCard`()
	{
		assertEquals(ServerCard(rank = Rank.ACE, suit = Suit.DIAMONDS), "A♢".toCard())
		assertEquals(ServerCard(rank = Rank.SIX, suit = Suit.DIAMONDS), "6♢".toCard())
		assertEquals(ServerCard(rank = Rank.EIGHT, suit = Suit.CLUBS), "8♣".toCard())
		assertEquals(ServerCard(rank = Rank.TEN, suit = Suit.HEARTS), "10♡".toCard())
		assertEquals(ServerCard(rank = Rank.JACK, suit = Suit.SPADES), "J♠".toCard())
		assertEquals(ServerCard(rank = Rank.QUEEN, suit = Suit.SPADES), "Q♠".toCard())
		assertEquals(ServerCard(rank = Rank.KING, suit = Suit.SPADES), "K♠".toCard())
	}
	
	@Test
	fun `Test Oracle agent responses`()
	{
		val states = readTestStates(Ruleset(declareSuitsOnEights = false))
		val expectedActions = File("src/test/resources/oracle_actions.txt").readLines()
		assertEquals(states.size, expectedActions.size)
		
		val agent = OracleAgent()
		val actionStrs = states.map { agent.getMove(it).toString() }
		assertContentEquals(expectedActions, actionStrs)
	}
}
