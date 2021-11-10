package misterbander.crazyeights.game.ai

import com.badlogic.gdx.math.MathUtils
import misterbander.crazyeights.game.GameState
import misterbander.crazyeights.game.Move

/**
 * A simple baseline agent. It chooses moves randomly.
 */
class BetterRandomAgent : Agent
{
	override fun getMove(state: GameState): Move
	{
		val moves = state.moves
		if (moves.size > 1)
			return moves[MathUtils.random(1, moves.size - 1)]
		return moves[0]
	}
}
