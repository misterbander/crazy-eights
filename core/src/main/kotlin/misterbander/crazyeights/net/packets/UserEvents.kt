package misterbander.crazyeights.net.packets

import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.User

@NoArg
data class UserJoinedEvent(val user: User)

@NoArg
data class UserLeftEvent(val user: User)
