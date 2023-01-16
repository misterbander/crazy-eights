package misterbander.crazyeights.net.packets

import misterbander.crazyeights.net.server.ServerCard.Suit

@NoArg
data class EightsPlayedEvent(val playerUsername: String) : PowerCardPlayedEvent
{
	override val delayMillis: Long
		get() = 0
}

@NoArg
data class SuitDeclareEvent(val suit: Suit)

@NoArg
data class DrawTwosPlayedEvent(val drawCardCount: Int) : PowerCardPlayedEvent
{
	override val delayMillis: Long
		get() = 2000
}

@NoArg
data class DrawTwoPenaltyEvent(val victimUsername: String, val drawCardCount: Int)

@NoArg
data class SkipsPlayedEvent(val victimUsername: String) : PowerCardPlayedEvent
{
	override val delayMillis: Long
		get() = 2500
}

@NoArg
data class ReversePlayedEvent(val isReversed: Boolean) : PowerCardPlayedEvent
{
	override val delayMillis: Long
		get() = 2000
}

data class DrawStackRefillEvent(val cardGroupChangeEvent: CardGroupChangeEvent? = null, val shuffleSeed: Long = 0)
