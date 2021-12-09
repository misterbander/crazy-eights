package misterbander.crazyeights.net.packets

import misterbander.crazyeights.model.GameState

data class NewGameEvent(
	val cardGroupChangeEvent: CardGroupChangeEvent? = null,
	val shuffleSeed: Long = 0,
	val gameState: GameState? = null
)

object NewGameActionFinishedEvent
