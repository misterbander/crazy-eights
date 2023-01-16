package misterbander.crazyeights.net.server.game.ai

import misterbander.crazyeights.net.server.game.Move
import misterbander.crazyeights.net.server.game.Player
import misterbander.crazyeights.net.server.game.ServerGameState

interface Agent : Player
{
	fun getMove(state: ServerGameState): Move
}
