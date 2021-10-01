package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.Align
import ktx.actors.plusAssign
import ktx.scene2d.*
import ktx.style.*
import misterbander.gframework.scene2d.GObject
import misterbander.sandboxtabletop.PLAYER_NAMETAG_LABEL_STYLE
import misterbander.sandboxtabletop.Room
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.scene2d.modules.SmoothMovable

class SandboxTabletopCursor(
	room: Room,
	user: User,
	noLabel: Boolean = false
) : GObject<SandboxTabletop>(room)
{
	private val base: TextureRegion
	private val border: TextureRegion
	
	// Modules
	private val smoothMovable = SmoothMovable(this)
	
	init
	{
		touchable = Touchable.disabled
		color = user.color
		base = Scene2DSkin.defaultSkin["cursorbase"]
		border = Scene2DSkin.defaultSkin["cursorborder"]
		setSize(base.regionWidth.toFloat(), base.regionHeight.toFloat())
		setPositionAndTargetPosition(room.uiViewport.minWorldWidth/2, room.uiViewport.minWorldHeight/2)
		if (!noLabel)
		{
			this += scene2d.label(user.username, PLAYER_NAMETAG_LABEL_STYLE).apply {
				pack()
				setPosition(this@SandboxTabletopCursor.width/2 - 3F, 0F, Align.topLeft)
			}
		}
		
		// Add modules
		this += smoothMovable
	}
	
	fun setTargetPosition(x: Float, y: Float) = smoothMovable.setTargetPosition(x - 3, y - height)
	
	fun setPositionAndTargetPosition(x: Float, y: Float) =
		smoothMovable.setPositionAndTargetPosition(x - 3, y - height)
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		batch.apply {
			color = Color.WHITE
			draw(border, x, y)
			color = this@SandboxTabletopCursor.color
			draw(base, x, y)
			color = Color.WHITE
		}
		super.draw(batch, parentAlpha)
	}
}
