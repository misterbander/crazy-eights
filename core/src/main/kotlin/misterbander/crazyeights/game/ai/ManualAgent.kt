package misterbander.crazyeights.game.ai

import ktx.collections.*
import misterbander.crazyeights.game.Move
import misterbander.crazyeights.game.ServerGameState

class ManualAgent(override val name: String = "ManualAgent") : Agent
{
	override fun getMove(state: ServerGameState): Move
	{
		val moves = state.moves
		println("Your hand:       ${state.currentPlayerHand.map { it.name }}")
		println("Available moves:")
		moves.forEachIndexed { index, move -> println("\t$index: $move") }
		if (state.declaredSuit != null)
			println("Declared suit = ${state.declaredSuit}")
		if (state.drawTwoEffectCardCount > 0)
			println("Draw penalty = ${state.drawTwoEffectCardCount}")
		while (true)
		{
			try
			{
				return moves[readLine()!!.toInt()]
			}
			catch (e: Exception)
			{
				e.printStackTrace()
			}
		}
	}
}
