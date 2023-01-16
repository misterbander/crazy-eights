package misterbander.crazyeights.net.packets

import misterbander.crazyeights.net.server.User

@NoArg
data class Chat(val user: User? = null, val message: String, val isSystemMessage: Boolean = false)
