package misterbander.crazyeights.model

import com.badlogic.gdx.graphics.Color

@NoArg
data class User(val username: String, val color: Color = Color(0F, 0F, 0F, 1F), val isAi: Boolean = false)
