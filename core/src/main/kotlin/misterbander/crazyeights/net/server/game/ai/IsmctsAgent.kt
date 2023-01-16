package misterbander.crazyeights.net.server.game.ai

import com.badlogic.gdx.math.CumulativeDistribution
import ktx.collections.*
import misterbander.crazyeights.net.server.game.DrawMove
import misterbander.crazyeights.net.server.game.Move
import misterbander.crazyeights.net.server.game.PassMove
import misterbander.crazyeights.net.server.game.ServerGameState

class IsmctsAgent(override val name: String = "IsmctsAgent") : Agent
{
	override fun getMove(state: ServerGameState): Move
	{
		val moves = state.moves
		if (moves.size == 1) // No need for tree search if there is only one move
			return moves[0]
		
		val rootNode = Node()
		repeat(2500) {
			var node = rootNode
			
			// Determinize
			val observedState = state.observedState
			val observedMoves = observedState.moves
			
			// Select
			while (!observedState.isTerminal && node.getUntriedMoves(observedMoves).isEmpty)
			{
				// Node is fully expanded and non-terminal
				node = node.selectUCB1(observedMoves)
				observedState.doMove(node.moveToNode!!)
			}
			
			// Expand
			val untriedMoves = node.getUntriedMoves(observedMoves)
			if (untriedMoves.isNotEmpty()) // Can we expand (i.e. state/node is non-terminal)?
			{
				val move = untriedMoves.random()
				val currentPlayer = observedState.currentPlayer
				observedState.doMove(move)
				node = node.addChild(move, currentPlayer) // Add child and descend tree
			}
			
			// Simulate
			while (!observedState.isTerminal)
			{
				val moveDistribution = CumulativeDistribution<Move>()
				observedMoves.forEach { move: Move -> moveDistribution.add(move, if (move is DrawMove || move is PassMove) 1F else 10000F) }
				moveDistribution.generateNormalized()
				observedState.doMove(moveDistribution.value())
			}
			
			// Backpropagate
			while (true) // Backpropagate from the expanded node and work back to the root node
			{
				node.update(observedState)
				node = node.parentNode ?: break
			}
		}
		
		// Return the move that was most visited
		return rootNode.children.maxByOrNull { node: Node -> node.visits }!!.moveToNode!!
	}
}
