package misterbander.sandboxtabletop.net

import com.esotericsoftware.kryo.util.Pool
import misterbander.sandboxtabletop.model.CursorPosition
import misterbander.sandboxtabletop.net.packets.ObjectMovedEvent

typealias KryoPool<T> = Pool<T>

typealias KryoPoolable = Pool.Poolable

inline fun <Type> kryoPool(crossinline provider: () -> Type): KryoPool<Type> =
	object : Pool<Type>( true, false)
	{
		override fun create(): Type = provider()
	}

val cursorPositionPool = kryoPool { CursorPosition() }
val objectMovedEventPool = kryoPool { ObjectMovedEvent() }
