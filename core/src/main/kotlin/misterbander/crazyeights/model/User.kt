package misterbander.crazyeights.model

import com.badlogic.gdx.graphics.Color
import misterbander.crazyeights.game.Player

@NoArg
data class User(
	override val name: String,
	val color: Color = Color(0F, 0F, 0F, 1F),
	val isAi: Boolean = false
) : Player
