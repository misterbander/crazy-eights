package misterbander.crazyeights.model

data class ServerCardHolder(
	override val id: Int = -1,
	override var x: Float = 0F,
	override var y: Float = 0F,
	override var rotation: Float = 0F,
	var cardGroup: ServerCardGroup = ServerCardGroup(),
	override var lockHolder: User? = null
) : ServerObject, ServerLockable
{
	override val canLock: Boolean
		get() = cardGroup.cards.isEmpty
	
	override fun toString(): String = "ServerCardHolder(id=$id, x=$x, y=$y, rotation=$rotation,\n        cardGroup=$cardGroup, lockholder=$lockHolder)"
}
