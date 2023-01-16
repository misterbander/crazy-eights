package misterbander.crazyeights.net.packets

import ktx.collections.*
import misterbander.crazyeights.net.server.ServerCard.Suit
import misterbander.crazyeights.net.server.game.Ruleset

@NoArg
data class GameState(
	val ruleset: Ruleset = Ruleset(),
	val players: GdxArray<String>,
	val currentPlayer: String,
	val isPlayReversed: Boolean,
	var drawCount: Int,
	val declaredSuit: Suit?,
	val drawTwoEffectCardCount: Int,
	val powerCardPlayedEvent: PowerCardPlayedEvent?
)
