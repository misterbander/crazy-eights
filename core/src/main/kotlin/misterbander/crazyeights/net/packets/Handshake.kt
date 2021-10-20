package misterbander.crazyeights.net.packets

import misterbander.crazyeights.VERSION_STRING

data class Handshake(val versionString: String = VERSION_STRING, val data: Array<String>? = null)
{
	override fun equals(other: Any?): Boolean
	{
		if (this === other)
			return true
		if (javaClass != other?.javaClass)
			return false
		
		other as Handshake
		
		if (versionString != other.versionString)
			return false
		if (data != null)
		{
			if (other.data == null)
				return false
			if (!data.contentEquals(other.data))
				return false
		}
		else if (other.data != null)
			return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		var result = versionString.hashCode()
		result = 31*result + (data?.contentHashCode() ?: 0)
		return result
	}
}
