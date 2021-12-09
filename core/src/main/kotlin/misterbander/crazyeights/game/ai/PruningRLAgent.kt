package misterbander.crazyeights.game.ai

import ktx.collections.*
import misterbander.crazyeights.game.ChangeSuitMove
import misterbander.crazyeights.game.DrawMove
import misterbander.crazyeights.game.DrawTwoEffectPenalty
import misterbander.crazyeights.game.Move
import misterbander.crazyeights.game.PassMove
import misterbander.crazyeights.game.PlayMove
import misterbander.crazyeights.game.Player
import misterbander.crazyeights.game.ServerGameState
import misterbander.crazyeights.model.ServerCard
import kotlin.math.max
import kotlin.math.min

class PruningRLAgent(override val name: String = "PruningAgent") : Agent
{
	// rl_weights
	private val rlCoefficients = doubleArrayOf(-9.800666370478867, 6.345468984316739, 10.699691747666808, 0.09546959149951917, 1.0870586436293268, 8.058630148009408, 48.47630741126674)
	// state_weights
	private val stateFeatureCoefficients = doubleArrayOf(-4.71147509645558E-06, -3.6665595549609968E-06, -3.071781964252339E-05, 1.6670161418462106E-06, -2.7561416865814155E-05, -1.1177873467151159E-05, 7.697915131362483E-05)
	// action_and_state_weights
	private val moveAndStateFeatureCoefficients = doubleArrayOf(-8.88493919889438E-07, -6.525682217057895E-07, -8.899537649336145E-07, 2.9570466742759173E-07, -1.7105942948250108E-06, -3.907499246949064E-07, 2.0703149360889496E-05, -3.6528275955061613E-06, 7.303523162358456E-05, -3.993787663875497E-06, -4.0301578035528655E-06, -3.991606167079328E-06, -3.994839477361488E-06, -3.981826702486002E-06, -3.973267375915404E-06, -4.01008739920423E-06, -7.237926801538167E-07, -3.981386217164895E-06, -3.997629768317886E-06, -3.986759106309432E-06, -3.977255710228937E-06, -4.036858595538313E-06, -1.2150828276238285E-05, -1.2173832802221336E-05, -1.2181763628301913E-05, -1.2172829960426631E-05)
	private var previousPlayer: Player = this
	private val depth = 2
	
	private val bestMoves = GdxArray<Move>()
	
	override fun getMove(state: ServerGameState): Move
	{
		bestMoves.clear()
		
		val observation = PruningRLAgentObservation(state)
		previousPlayer = observation.previousPlayer
		val moves = state.moves
		assert(moves == observation.moves) { "Inconsistent moves! Expecting $moves but got ${observation.moves}" }
		val moveValueMap = moves.toGdxMap(
			keyProvider = { it },
			valueProvider = {
				miniMax(
					observation.afterMove(it),
					depth,
					observation.nextPlayer,
					Double.NEGATIVE_INFINITY,
					Double.POSITIVE_INFINITY
				)
			}
		)
		val maxValue = moves.maxOf { moveValueMap[it]!! }
		for ((move, value) in moveValueMap)
		{
			if (value == maxValue)
				bestMoves += move
		}
		return bestMoves.random()
	}
	
	/**
	 * Standard minimax algorithm.
	 */
	private fun miniMax(observation: PruningRLAgentObservation, depth: Int, player: Player, startAlpha: Double, startBeta: Double): Double
	{
		var alpha = startAlpha
		var beta = startBeta
		
		if (observation.isTerminal)
			return observation.result
		if (depth == 0)
			return observation.evaluate()
		if (player == this) // I am the maximizing player
		{
			var maxValue = Double.NEGATIVE_INFINITY
			val moves = observation.moves
			for (move in moves)
			{
				val afterMove = observation.afterMove(move)
				val nextPlayer = afterMove.currentPlayer
				val value = miniMax(afterMove, depth, nextPlayer, alpha, beta)
				maxValue = max(maxValue, value)
				alpha = max(alpha, value)
				if (alpha >= beta)
					break
			}
			return maxValue
		}
		var minValue = Double.POSITIVE_INFINITY
		val moves = getMostProbableMoves(observation, 10)
		for (move in moves)
		{
			val afterMove = observation.afterMove(move)
			val nextPlayer = observation.currentPlayer
			val value = miniMax(afterMove, if (player != previousPlayer) depth else depth - 1, nextPlayer, alpha, beta)
			minValue = min(minValue, value)
			beta = min(beta, value)
			if (alpha >= beta)
				break
		}
		return minValue
	}
	
	private fun getMostProbableMoves(observation: PruningRLAgentObservation, numberOfMoves: Int): GdxArray<Move>
	{
		val mostProbableMoves = GdxArray(observation.moves)
		val moveWeightMap = mostProbableMoves.toGdxMap(
			keyProvider = { it },
			valueProvider = { getTransitionWeight(observation, it) }
		)
		mostProbableMoves.sortBy { moveWeightMap[it]!! }
		if (mostProbableMoves.size > numberOfMoves)
			mostProbableMoves.removeRange(numberOfMoves, mostProbableMoves.size - 1)
		return mostProbableMoves
	}
	
	private fun getTransitionWeight(observation: PruningRLAgentObservation, move: Move): Double
	{
		val stateFeatures = observation.extractStateFeatures()
		val moveFeatures = move.extractMoveFeatures()
		val stateWeight = stateFeatureCoefficients.dot(stateFeatures)
		val moveAndStateWeight = moveAndStateFeatureCoefficients.dot(stateFeatures + moveFeatures)
		return moveAndStateWeight/stateWeight
	}
	
	private fun PruningRLAgentObservation.evaluate(): Double = rlCoefficients.dot(extractStateFeatures())
	
	private fun DoubleArray.dot(array: DoubleArray): Double
	{
		assert(size == array.size)
		var sum = 0.0
		for (i in indices)
			sum += this[i]*array[i]
		return sum
	}
	
	private fun PruningRLAgentObservation.extractStateFeatures(): DoubleArray
	{
		val observerCardCount = playerCardCounts[observer]!!
		val opponentsCardCount = playerCardCounts.values().sumOf { it } - observerCardCount
		val observerEightsCount = observerHand.count { it.rank == ServerCard.Rank.EIGHT }
		val numberOfCardsWithSameRank = observerHand.count { it.rank == topCard.rank }
		val numberOfCardsWithSameSuit = observerHand.count { it.suit == topCard.suit }
		
		return doubleArrayOf(
			observerCardCount.toDouble(),
			opponentsCardCount.toDouble(),
			observerEightsCount.toDouble(),
			drawStackCardCount.toDouble(),
			numberOfCardsWithSameRank.toDouble(),
			numberOfCardsWithSameSuit.toDouble(),
			1.0
		)
	}
	
	private fun Move.extractMoveFeatures(): DoubleArray
	{
		val moveFeature = doubleArrayOf(
			if (this is PassMove) 1.0 else 0.0,
			if (this is DrawMove || this is DrawTwoEffectPenalty) 1.0 else 0.0
		)
		val rankFeature = DoubleArray(13)
		val suitFeature = when (this)
		{
			is PlayMove ->
			{
				rankFeature[card.rank.ordinal - 1] = 1.0
				doubleArrayOf(
					if (card.suit == ServerCard.Suit.HEARTS) 1.0 else 0.0,
					if (card.suit == ServerCard.Suit.DIAMONDS) 1.0 else 0.0,
					if (card.suit == ServerCard.Suit.CLUBS) 1.0 else 0.0,
					if (card.suit == ServerCard.Suit.SPADES) 1.0 else 0.0
				)
			}
			is ChangeSuitMove ->
			{
				rankFeature[card.rank.ordinal - 1] = 1.0
				doubleArrayOf(
					if (card.suit == ServerCard.Suit.HEARTS) 1.0 else 0.0,
					if (card.suit == ServerCard.Suit.DIAMONDS) 1.0 else 0.0,
					if (card.suit == ServerCard.Suit.CLUBS) 1.0 else 0.0,
					if (card.suit == ServerCard.Suit.SPADES) 1.0 else 0.0
				)
			}
			else -> DoubleArray(4)
		}
		return moveFeature + rankFeature + suitFeature
	}
}
