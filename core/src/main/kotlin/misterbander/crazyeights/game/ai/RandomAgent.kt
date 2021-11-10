package misterbander.crazyeights.game.ai

import misterbander.crazyeights.game.GameState
import misterbander.crazyeights.game.Move

/**
 * A simple baseline agent. It chooses moves randomly.
 */
class RandomAgent : Agent
{
	override fun getMove(state: GameState): Move = state.moves.random()
}
