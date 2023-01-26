package misterbander.crazyeights.net.server.game.ai

import ktx.collections.*
import misterbander.crazyeights.net.server.game.Move
import misterbander.crazyeights.net.server.game.Player
import misterbander.crazyeights.net.server.game.ServerGameState
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * A node in the game tree. Note that `wins` is always from the viewpoint of `playerJustMoved`.
 */
sealed class Node
{
	val children = GdxArray<ChildNode>()
	protected var wins = 0
	var visits = 0
		private set
	protected var availability = 1
	
	/**
	 * Return the elements of `legalMoves` for which this node does not have children.
	 */
	fun getUntriedMoves(legalMoves: GdxArray<Move>): GdxArray<Move>
	{
		// Find all moves for which this node *does* have children
		val triedMoves: GdxArray<Move> = children.map { it.moveToNode }
		
		// Return all moves that are legal but have not been tried yet
		val untriedMoves = GdxArray(legalMoves)
		untriedMoves -= triedMoves
		return untriedMoves
	}
	
	/**
	 * Use the UCB1 formula to select a child node, filtered by the given list of legal moves.
	 * exploration is a constant balancing between exploitation and exploration, with default value 0.7 (approximately sqrt(2) / 2)
	 */
	fun selectUCB1(legalMoves: GdxArray<Move>, exploration: Float = 0.7F): ChildNode
	{
		// Filter the list of children by the list of legal moves
		val legalChildren = GdxArray<ChildNode>()
		for (node: ChildNode in children)
		{
			if (node.moveToNode in legalMoves)
				legalChildren += node
		}
		
		// Get the child with the highest UCB score
		val bestNode = legalChildren.maxByOrNull { node ->
			node.wins.toFloat()/node.visits + exploration*sqrt(ln(node.availability.toFloat())/node.visits)
		}!!
		
		// Update availability counts. It is easier to do this now than during backpropagation
		for (legalChild: ChildNode in legalChildren)
			legalChild.availability++
		
		return bestNode
	}
	
	/**
	 * Add a new child node for the move m.
	 * @return The added child node
	 */
	fun addChild(moveToNode: Move, playerJustMoved: Player): ChildNode
	{
		val node = ChildNode(this, moveToNode, playerJustMoved)
		children += node
		return node
	}
	
	/**
	 * Update this node - increment the visit count by one, and increase the win count by the result of `terminalState` for `playerJustMoved`.
	 */
	open fun update(terminalState: ServerGameState)
	{
		visits++
	}
}

class RootNode : Node()

/**
 * @property parentNode the parent node
 * @property moveToNode the move that got us to this node
 * @property playerJustMoved the only part of the state that the Node needs later
 */
class ChildNode(
	val parentNode: Node,
	val moveToNode: Move,
	private val playerJustMoved: Player
) : Node()
{
	override fun update(terminalState: ServerGameState)
	{
		super.update(terminalState)
		wins += terminalState.getResult(playerJustMoved)
	}
}
