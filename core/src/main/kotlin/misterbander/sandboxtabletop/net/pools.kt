package misterbander.sandboxtabletop.net

import com.esotericsoftware.kryo.util.Pool
import ktx.log.debug
import misterbander.sandboxtabletop.model.CursorPosition
import misterbander.sandboxtabletop.net.packets.ObjectMovedEvent

typealias KryoPool<T> = Pool<T>

typealias KryoPoolable = Pool.Poolable

inline fun <reified Type> kryoPool(crossinline provider: () -> Type): KryoPool<Type> =
	object : Pool<Type>( true, false)
	{
		private var count = 0
		
		override fun create(): Type
		{
			count++
			debug("KryoPool | DEBUG") { "Max instance count ${Type::class.java} = $count" }
			return provider()
		}
	}

val cursorPositionPool = kryoPool { CursorPosition() }
val objectMovedEventPool = kryoPool { ObjectMovedEvent() }
