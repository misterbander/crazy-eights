package misterbander.crazyeights.game

import misterbander.crazyeights.model.ServerCard.Rank

data class Ruleset(
	val passAfterDraw: Boolean = false,
	val maxDrawCount: Int = 3,
	val declareSuitsOnEights: Boolean = true,
	val drawTwos: Rank? = Rank.TWO,
	val skips: Rank? = Rank.QUEEN,
	val reverses: Rank? = Rank.ACE,
	val firstDiscardOnDealTriggersPower: Boolean = false
)
