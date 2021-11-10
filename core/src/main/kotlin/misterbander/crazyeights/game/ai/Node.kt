package misterbander.crazyeights.game.ai

import ktx.collections.*
import misterbander.crazyeights.game.GameState
import misterbander.crazyeights.game.Move
import misterbander.crazyeights.game.Player
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * A node in the game tree. Note that `wins` is always from the viewpoint of `playerJustMoved`.
 * @property parentNode null for the root node
 * @property moveToNode the move that got us to this node. Null for the root node
 * @property playerJustMoved The only part of the state that the Node needs later
 */
class Node(
	val parentNode: Node? = null,
	val moveToNode: Move? = null,
	private val playerJustMoved: Player? = null
)
{
	val children = GdxArray<Node>()
	private var wins = 0
	var visits = 0
		private set
	private var availability = 1
	
	/**
	 * Return the elements of `legalMoves` for which this node does not have children.
	 */
	fun getUntriedMoves(legalMoves: GdxArray<Move>): GdxArray<Move>
	{
		// Find all moves for which this node *does* have children
		val triedMoves: GdxArray<Move> = children.map { it.moveToNode!! }
		
		// Return all moves that are legal but have not been tried yet
		val untriedMoves = GdxArray(legalMoves)
		untriedMoves -= triedMoves
		return untriedMoves
	}
	
	/**
	 * Use the UCB1 formula to select a child node, filtered by the given list of legal moves.
	 * exploration is a constant balancing between exploitation and exploration, with default value 0.7 (approximately sqrt(2) / 2)
	 */
	fun selectUCB1(legalMoves: GdxArray<Move>, exploration: Float = 0.7F): Node
	{
		// Filter the list of children by the list of legal moves
		val legalChildren = GdxArray<Node>()
		for (node: Node in children)
		{
			if (node.moveToNode in legalMoves)
				legalChildren += node
		}
		
		// Get the child with the highest UCB score
		val bestNode = legalChildren.maxByOrNull { node ->
			node.wins.toFloat()/node.visits + exploration*sqrt(ln(node.availability.toFloat())/node.visits)
		}!!
		
		// Update availability counts. It is easier to do this now than during backpropagation
		for (legalChild: Node in legalChildren)
			legalChild.availability++
		
		return bestNode
	}
	
	/**
	 * Add a new child node for the move m.
	 * @return The added child node
	 */
	fun addChild(moveToNode: Move, playerJustMoved: Player): Node
	{
		val node = Node(this, moveToNode, playerJustMoved)
		children += node
		return node
	}
	
	/**
	 * Update this node - increment the visit count by one, and increase the win count by the result of `terminalState` for `playerJustMoved`.
	 */
	fun update(terminalState: GameState)
	{
		visits++
		if (playerJustMoved != null)
			wins += terminalState.getResult(playerJustMoved)
	}
}
