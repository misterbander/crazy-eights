package misterbander.crazyeights.net.packets

import ktx.collections.*
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.ServerObject

@NoArg
data class ObjectOwnEvent(val id: Int, val ownerUsername: String)

@NoArg
data class ObjectDisownEvent(
	val id: Int = -1,
	val x: Float = 0F,
	val y: Float = 0F,
	val rotation: Float = 0F,
	val isFaceUp: Boolean = false,
	val disownerUsername: String
)

@NoArg
data class HandUpdateEvent(val hand: GdxArray<ServerObject>, val ownerUsername: String)
