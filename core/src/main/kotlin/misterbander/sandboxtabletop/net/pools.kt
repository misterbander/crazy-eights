package misterbander.sandboxtabletop.net

import com.esotericsoftware.kryo.util.Pool
import misterbander.sandboxtabletop.model.CursorPosition

inline fun <Type> kryoPool(crossinline provider: () -> Type): Pool<Type> =
	object : Pool<Type>( true, false)
	{
		override fun create(): Type = provider()
	}

val cursorPositionPool = kryoPool { CursorPosition() }
