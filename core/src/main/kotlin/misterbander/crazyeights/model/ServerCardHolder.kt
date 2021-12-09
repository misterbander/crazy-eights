package misterbander.crazyeights.model

data class ServerCardHolder(
	override val id: Int = -1,
	override var x: Float = 0F,
	override var y: Float = 0F,
	override var rotation: Float = 0F,
	var cardGroup: ServerCardGroup = ServerCardGroup(),
	override var lockHolder: String? = null
) : ServerObject, ServerLockable
{
	val defaultType = cardGroup.type
	
	override val canLock: Boolean
		get() = false
	
	init
	{
		cardGroup.cardHolderId = id
	}
	
	override fun toString(): String = "ServerCardHolder(id=$id, x=$x, y=$y, rotation=$rotation,\n        cardGroup=$cardGroup, lockholder=$lockHolder)"
}
