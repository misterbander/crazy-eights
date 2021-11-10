package misterbander.crazyeights.game.ai

import misterbander.crazyeights.game.GameState
import misterbander.crazyeights.game.Move
import misterbander.crazyeights.game.Player

interface Agent : Player
{
	fun getMove(state: GameState): Move
}
