package misterbander.crazyeights.scene2d

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.Align
import ktx.actors.plusAssign
import ktx.scene2d.*
import ktx.style.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.LABEL_TINY_STYLE
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.model.User
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.scene2d.GObject

class CrazyEightsCursor(
	room: RoomScreen,
	user: User,
	noLabel: Boolean = false
) : GObject<CrazyEights>(room)
{
	private val base: TextureRegion
	private val border: TextureRegion
	
	// Modules
	private val smoothMovable = SmoothMovable(this)
	
	init
	{
		touchable = Touchable.disabled
		color = user.color
		base = Scene2DSkin.defaultSkin["cursor_base"]
		border = Scene2DSkin.defaultSkin["cursor_border"]
		setSize(base.regionWidth.toFloat(), base.regionHeight.toFloat())
		snapPosition(room.uiViewport.minWorldWidth/2, room.uiViewport.minWorldHeight/2)
		if (!noLabel)
		{
			this += scene2d.container {
				background = Scene2DSkin.defaultSkin["background"]
				pad(2F, 12F, 2F, 12F)
				label(user.name, LABEL_TINY_STYLE) { pack() }
				pack()
				setPosition(this@CrazyEightsCursor.width/2 - 3F, 0F, Align.topLeft)
			}
		}
		setOrigin(3F, height)
		
		// Add modules
		this += smoothMovable
	}
	
	override fun setPosition(x: Float, y: Float) = smoothMovable.setPosition(x - originX, y - originY)
	
	fun snapPosition(x: Float, y: Float) =
		smoothMovable.snapPosition(x - originX, y - originY)
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		batch.apply {
			color = Color.WHITE
			draw(border, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
			color = this@CrazyEightsCursor.color
			draw(base, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
			color = Color.WHITE
		}
		super.draw(batch, parentAlpha)
	}
}
