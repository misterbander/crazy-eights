package misterbander.crazyeights.net.packets

data class ObjectLockEvent(val id: Int = -1, val lockerUsername: String = "")

data class ObjectUnlockEvent(val id: Int = -1, val unlockerUsername: String = "")
