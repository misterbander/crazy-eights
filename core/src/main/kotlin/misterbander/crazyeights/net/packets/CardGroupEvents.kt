package misterbander.crazyeights.net.packets

import misterbander.crazyeights.model.NoArg

@NoArg
data class CardGroupCreateEvent(
	val id: Int = -1,
	val cardIds: IntArray,
	val cardRotations: FloatArray
)
{
	override fun equals(other: Any?): Boolean
	{
		if (this === other)
			return true
		if (javaClass != other?.javaClass)
			return false
		
		other as CardGroupCreateEvent
		
		if (id != other.id)
			return false
		if (!cardIds.contentEquals(other.cardIds))
			return false
		if (!cardRotations.contentEquals(other.cardRotations))
			return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		var result = id
		result = 31*result + cardIds.contentHashCode()
		result = 31*result + cardRotations.contentHashCode()
		return result
	}
}

@NoArg
data class CardGroupChangeEvent(
	val cardIds: IntArray,
	val cardRotations: FloatArray,
	val newCardGroupId: Int,
	val changerUsername: String
)
{
	override fun equals(other: Any?): Boolean
	{
		if (this === other)
			return true
		if (javaClass != other?.javaClass)
			return false
		
		other as CardGroupChangeEvent
		
		if (!cardIds.contentEquals(other.cardIds))
			return false
		if (!cardRotations.contentEquals(other.cardRotations))
			return false
		if (newCardGroupId != other.newCardGroupId)
			return false
		if (changerUsername != other.changerUsername)
			return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		var result = cardIds.contentHashCode()
		result = 31*result + cardRotations.contentHashCode()
		result = 31*result + newCardGroupId
		result = 31*result + changerUsername.hashCode()
		return result
	}
}

@NoArg
data class CardGroupDetachEvent(val cardHolderId: Int, val replacementCardGroupId: Int = -1, val changerUsername: String)

@NoArg
data class CardGroupDismantleEvent(val id: Int)
