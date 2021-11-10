package misterbander.crazyeights.game

data class Ruleset(
	val passAfterDraw: Boolean = false,
	val declareSuitsOnEights: Boolean = true,
	val drawTwos: Boolean = false,
	val skips: Boolean = false,
	val reverses: Boolean = false
)
