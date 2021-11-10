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
	
	@Test
	fun `Test smallest element not less than target`()
	{
		val nums = intArrayOf(3, 7, 13, 14, 24, 35, 36, 38)
		fun bs(target: Int): Int
		{
			// Perform binary search to select the smallest index with cumulative weight greater than or
			// equal to the randomly chosen number
			var low = 0
			var high = nums.lastIndex
			while (low != high)
			{
				val mid = (low + high)/2
				if (nums[mid] == target)
					return nums[mid]
				if (nums[mid] > target)
					high = mid
				else
					low = mid + 1
			}
			return nums[low]
		}
		for (i in 1..3)
			assertEquals(3, bs(i))
		for (i in 4..7)
			assertEquals(7, bs(i))
		for (i in 8..13)
			assertEquals(13, bs(i))
		assertEquals(14, bs(14))
		for (i in 15..24)
			assertEquals(24, bs(i))
		for (i in 25..35)
			assertEquals(35, bs(i))
		assertEquals(36, bs(36))
		for (i in 37..39)
			assertEquals(38, bs(i))
	}
}
