package misterbander.gframework.util

import com.badlogic.gdx.Gdx
import ktx.log.debug
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.collections.set

/**
 * Maps string keys to serializable objects that can be saved to file storage. Useful for saving game state.
 * @author Mister_Bander
 */
class PersistentStateMapper
{
	val stateMap: HashMap<String, Serializable> = HashMap()
	
	inline operator fun <reified T : Serializable> get(key: String): T?
	{
		val value = stateMap[key] as? T?
		if (stateMap[key] !is T)
			debug("PersistentStateMapper | DEBUG") { "Mapping for \"$key\" is not an instance of ${T::class.java.name}" }
		else if (value == null)
			debug("PersistentStateMapper | DEBUG") { "Mapping for \"$key\" does not exist" }
		return value
	}
	
	operator fun set(key: String, value: Serializable)
	{
		debug("PersistentStateMapper | DEBUG") { "Mapping $key to $value" }
		stateMap[key] = value
	}
	
	/**
	 * Reads state from file storage.
	 * @return True if state is successfully read, false if file does not exist.
	 */
	@Suppress("UNCHECKED_CAST")
	fun read(filePath: String): Boolean
	{
		val stateFile = Gdx.files.local(filePath)
		if (stateFile.exists())
		{
			val inputStream = ObjectInputStream(stateFile.read())
			stateMap.clear()
			stateMap.putAll(inputStream.readObject() as HashMap<String, Serializable>)
			inputStream.close()
			return true
		}
		return false
	}
	
	/**
	 * Reads state from stream.
	 */
	@Suppress("UNCHECKED_CAST")
	fun read(inputStream: ObjectInputStream)
	{
		stateMap.clear()
		stateMap.putAll(inputStream.readObject() as HashMap<String, Serializable>)
	}
	
	/**
	 * Writes state to file storage.
	 */
	fun write(filePath: String)
	{
		val stateFile = Gdx.files.local(filePath)
		val outputStream = ObjectOutputStream(stateFile.write(false))
		outputStream.writeObject(stateMap)
		outputStream.close()
	}
	
	fun write(outputStream: ObjectOutputStream)
	{
		outputStream.writeObject(stateMap)
	}
}
