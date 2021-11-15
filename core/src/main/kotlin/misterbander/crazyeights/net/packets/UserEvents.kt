package misterbander.crazyeights.net.packets

import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.User

@NoArg
data class UserJoinEvent(val user: User)

@NoArg
data class UserLeaveEvent(val user: User)
