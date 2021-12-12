package misterbander.crazyeights.scene2d

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import ktx.actors.alpha
import ktx.actors.plusAssign
import ktx.math.component1
import ktx.math.component2
import misterbander.crazyeights.CrazyEights
import misterbander.gframework.GScreen
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec
import kotlin.math.min

class PlayDirectionIndicator(room: GScreen<CrazyEights>) : GObject<CrazyEights>(room)
{
	private val radius = 240F
	private val triangleSize = 40F
	private val thickness = 40F
	private var rotationSpeed = 20F
	
	private val darkRed = Color(0x351A1AFF)
	
	init
	{
		color = darkRed
		setPosition(640F, 360F)
		alpha = 0F
	}
	
	override fun update(delta: Float)
	{
		rotationSpeed = min(rotationSpeed + 10*delta, 20F)
		if (rotationSpeed > 0)
			rotation += if (scaleX == 1F) -rotationSpeed*delta else rotationSpeed*delta
	}
	
	fun flipDirection()
	{
		scaleX = -scaleX
		rotationSpeed = -20F
		color = Color.WHITE
		this += delay(0.2F, color(darkRed, 0.5F))
	}
	
	fun reset()
	{
		clearActions()
		scaleX = 1F
		rotationSpeed = 20F
		color = darkRed
		alpha = 0F
	}
	
	override fun drawChildren(batch: Batch, parentAlpha: Float)
	{
		val arrowAngle1 = 20F
		val arrowAngle2 = -160F
		
		tempVec.set(radius, 0F)
		val (ox1, oy1) = tempVec.setAngleDeg(arrowAngle1)
		tempVec.setLength(triangleSize)
		val (ax1, ay1) = tempVec.rotateDeg(-90F)
		val (bx1, by1) = tempVec.rotateDeg(120F)
		val (cx1, cy1) = tempVec.rotateDeg(120F)
		
		tempVec.set(radius, 0F)
		val (ox2, oy2) = tempVec.setAngleDeg(arrowAngle2)
		tempVec.setLength(triangleSize)
		val (ax2, ay2) = tempVec.rotateDeg(-90F)
		val (bx2, by2) = tempVec.rotateDeg(120F)
		val (cx2, cy2) = tempVec.rotateDeg(120F)
		
		game.shapeDrawer.apply {
			setColor(color)
			arc(0F, 0F, radius, arrowAngle1*MathUtils.degRad, 140*MathUtils.degRad, thickness)
			filledTriangle(
				ox1 + ax1, oy1 + ay1,
				ox1 + bx1, oy1 + by1,
				ox1 + cx1, oy1 + cy1
			)
			arc(0F, 0F, radius, arrowAngle2*MathUtils.degRad, 140*MathUtils.degRad, thickness)
			filledTriangle(
				ox2 + ax2, oy2 + ay2,
				ox2 + bx2, oy2 + by2,
				ox2 + cx2, oy2 + cy2
			)
		}
	}
}
