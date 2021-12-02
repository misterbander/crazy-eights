package misterbander.crazyeights.net.packets

import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.User

@NoArg
data class UserJoinedEvent(val user: User)

@NoArg
data class UserLeftEvent(val user: User)

@NoArg
data class SwapSeatsEvent(val username1: String, val username2: String)

object AiAddEvent

@NoArg
data class AiRemoveEvent(val username: String)
