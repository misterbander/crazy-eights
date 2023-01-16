package misterbander.crazyeights.net.packets

data class NewGameEvent(
	val cardGroupChangeEvent: CardGroupChangeEvent? = null,
	val shuffleSeed: Long = 0,
	val gameState: GameState? = null
)

@NoArg
data class GameEndedEvent(val winner: String)

object PassEvent
