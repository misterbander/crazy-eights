package misterbander.crazyeights.net.packets

data class ResetDeckEvent(val cardGroupChangeEvent: CardGroupChangeEvent? = null, val shuffleSeed: Long = 0)
