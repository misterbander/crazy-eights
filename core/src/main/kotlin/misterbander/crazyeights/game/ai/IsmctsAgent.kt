package misterbander.crazyeights.game.ai

import com.badlogic.gdx.utils.ObjectIntMap
import ktx.collections.*
import misterbander.crazyeights.game.DrawMove
import misterbander.crazyeights.game.GameState
import misterbander.crazyeights.game.Move
import misterbander.crazyeights.game.PassMove
import misterbander.gframework.util.weightedRandom

class IsmctsAgent : Agent
{
	override fun getMove(state: GameState): Move
	{
		val moves = state.moves
		if (moves.size == 1) // No need for tree search if there is only one move
			return moves[0]
		
		val rootNode = Node()
		for (i in 0 until 2500)
		{
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
				val moveWeightMap = ObjectIntMap<Move>()
				observedMoves.forEach { move: Move -> moveWeightMap.put(move, if (move is DrawMove || move is PassMove) 1 else 10000) }
				observedState.doMove(moveWeightMap.weightedRandom())
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
