package misterbander.crazyeights.net.packets

@NoArg
data class ObjectLockEvent(val id: Int, val lockerUsername: String)

@NoArg
data class ObjectUnlockEvent(val id: Int, val unlockerUsername: String, val sideEffects: Boolean = true)
