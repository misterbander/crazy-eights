package misterbander.crazyeights.net.server

import com.badlogic.gdx.graphics.Color
import misterbander.crazyeights.net.packets.NoArg
import misterbander.crazyeights.net.server.game.Player

@NoArg
data class User(
	override val name: String,
	val color: Color = Color(0F, 0F, 0F, 1F),
	val isAi: Boolean = false
) : Player
