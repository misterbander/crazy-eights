package misterbander.crazyeights.model

import ktx.collections.*
import misterbander.crazyeights.game.Ruleset
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.net.packets.PowerCardPlayedEvent

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
