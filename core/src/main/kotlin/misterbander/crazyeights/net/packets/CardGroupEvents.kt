package misterbander.crazyeights.net.packets

import misterbander.crazyeights.model.NoArg

data class CardGroupCreatedEvent(val id: Int = -1, val cardIds: IntArray = intArrayOf())
{
	override fun equals(other: Any?): Boolean
	{
		if (this === other)
			return true
		if (javaClass != other?.javaClass)
			return false
		
		other as CardGroupCreatedEvent
		
		if (id != other.id)
			return false
		if (!cardIds.contentEquals(other.cardIds))
			return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		var result = id
		result = 31*result + cardIds.contentHashCode()
		return result
	}
}

@NoArg
data class CardGroupChangedEvent(
	val cardIds: IntArray,
	val newCardGroupId: Int,
	val changerUsername: String
)
{
	override fun equals(other: Any?): Boolean
	{
		if (this === other) return true
		if (javaClass != other?.javaClass)
			return false
		
		other as CardGroupChangedEvent
		
		if (!cardIds.contentEquals(other.cardIds))
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
		result = 31*result + newCardGroupId
		result = 31*result + changerUsername.hashCode()
		return result
	}
}

@NoArg
data class CardGroupDismantledEvent(val id: Int, val dismantlerUsername: String)
