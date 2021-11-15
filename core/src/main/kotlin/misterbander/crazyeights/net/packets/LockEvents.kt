package misterbander.crazyeights.net.packets

import misterbander.crazyeights.model.NoArg

@NoArg
data class ObjectLockEvent(val id: Int, val lockerUsername: String)

@NoArg
data class ObjectUnlockEvent(val id: Int, val unlockerUsername: String)
