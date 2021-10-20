package misterbander.crazyeights.model

data class Chat(val user: User = User(), val message: String = "", val isSystemMessage: Boolean = false)
