package misterbander.crazyeights.net.packets

import misterbander.crazyeights.model.User

data class UserJoinEvent(val user: User = User())

data class UserLeaveEvent(val user: User = User())
