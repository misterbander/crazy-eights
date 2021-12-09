package misterbander.crazyeights.model

import ktx.collections.*
import misterbander.crazyeights.game.Ruleset
import misterbander.crazyeights.model.ServerCard.Suit

@NoArg
data class GameState(
	val ruleset: Ruleset = Ruleset(),
	val players: GdxArray<String>,
	var currentPlayer: String,
	var isPlayReversed: Boolean,
	var drawCount: Int,
	var declaredSuit: Suit?,
	var drawTwoEffectCardCount: Int
)
