import misterbander.crazyeights.game.Ruleset
import misterbander.crazyeights.game.ai.OracleAgent
import misterbander.crazyeights.game.toCard
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit
import org.junit.Test
import java.io.File
import java.nio.file.Paths
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
		val path = Paths.get("").toAbsolutePath().toString()
		println(path)
		val states = readTestStates(Ruleset(declareSuitsOnEights = false))
		val expectedActions = File("test/oracle_actions.txt").readLines()
		
		assertEquals(states.size, expectedActions.size)
		val agent = OracleAgent()
		
		for (i in states.indices)
		{
			val actionStr = agent.getMove(states[i]).toString()
			assertEquals(expectedActions[i], actionStr, "Action does not match for Game #${i + 1}")
		}
	}
}
