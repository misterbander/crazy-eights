package misterbander.crazyeights.net

import com.esotericsoftware.kryo.util.Pool
import ktx.log.debug
import misterbander.crazyeights.model.CursorPosition
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.net.packets.ObjectRotateEvent

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
val objectMoveEventPool = kryoPool { ObjectMoveEvent() }
val objectRotateEventPool = kryoPool { ObjectRotateEvent() }
