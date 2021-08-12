package misterbander.sandboxtabletop.model

import java.util.UUID

data class Chat(val user: User, val message: String, val isSystemMessage: Boolean)
{
	@Suppress("UNUSED")
	private constructor() : this(User(UUID(0, 0), ""), "", false)
}
