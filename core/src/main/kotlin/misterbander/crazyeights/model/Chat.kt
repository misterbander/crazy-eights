package misterbander.crazyeights.model

@NoArg
data class Chat(val user: User, val message: String, val isSystemMessage: Boolean = false)
