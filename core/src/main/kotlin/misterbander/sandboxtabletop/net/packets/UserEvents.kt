package misterbander.sandboxtabletop.net.packets

import misterbander.sandboxtabletop.model.User

data class UserJoinEvent(val user: User = User())

data class UserLeaveEvent(val user: User = User())
