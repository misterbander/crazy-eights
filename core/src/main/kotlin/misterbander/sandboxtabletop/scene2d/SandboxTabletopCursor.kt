package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import ktx.actors.plusAssign
import ktx.scene2d.*
import ktx.style.*
import misterbander.sandboxtabletop.PLAYER_NAMETAG_LABEL_STYLE
import misterbander.sandboxtabletop.RoomScreen
import misterbander.sandboxtabletop.model.User

class SandboxTabletopCursor(roomScreen: RoomScreen, user: User, noLabel: Boolean = false) : SmoothMovable()
{
	private val base: TextureRegion
	private val border: TextureRegion
	
	init
	{
		color = user.color
		base = Scene2DSkin.defaultSkin["cursorbase"]
		border = Scene2DSkin.defaultSkin["cursorborder"]
		setSize(base.regionWidth.toFloat(), base.regionHeight.toFloat())
		xInterpolator.set(roomScreen.uiViewport.minWorldWidth/2 - 3)
		yInterpolator.set(roomScreen.uiViewport.minWorldHeight/2 - height)
		if (!noLabel)
		{
			this += scene2d.label(user.username, PLAYER_NAMETAG_LABEL_STYLE).apply {
				pack()
				setPosition(this@SandboxTabletopCursor.width/2 - 3F, 0F, Align.topLeft)
			}
		}
	}
	
	override fun setTargetPosition(x: Float, y: Float) = super.setTargetPosition(x - 3, y - height)
	
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
