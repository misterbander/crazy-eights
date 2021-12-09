package misterbander.crazyeights.game.ai

import misterbander.crazyeights.game.Move
import misterbander.crazyeights.game.ServerGameState

/**
 * A simple baseline agent. It chooses moves randomly.
 */
class RandomAgent(override val name: String = "RandomAgent") : Agent
{
	override fun getMove(state: ServerGameState): Move = state.moves.random()
}
