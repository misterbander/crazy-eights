package misterbander.crazyeights.net.packets

data class NewGameEvent(
	val cardGroupChangeEvent: CardGroupChangeEvent? = null,
	val shuffleSeed: Long = 0
)
