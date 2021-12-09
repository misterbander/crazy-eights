package misterbander.crazyeights.game.ai

import misterbander.crazyeights.game.Move
import misterbander.crazyeights.game.Player
import misterbander.crazyeights.game.ServerGameState

interface Agent : Player
{
	fun getMove(state: ServerGameState): Move
}
