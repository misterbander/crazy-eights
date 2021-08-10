package misterbander.sandboxtabletop.model

import java.util.UUID

data class User(val uuid: UUID, val username: String)
{
	@Suppress("UNUSED")
	private constructor() : this(UUID(0, 0), "")
}
