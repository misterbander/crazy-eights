package misterbander.sandboxtabletop.net.packets

import misterbander.sandboxtabletop.model.User
import java.util.UUID

data class UserJoinEvent(val user: User)
{
	@Suppress("UNUSED")
	private constructor() : this(User(UUID.randomUUID(), ""))
}

data class UserLeaveEvent(val user: User)
{
	@Suppress("UNUSED")
	private constructor() : this(User(UUID.randomUUID(), ""))
}
