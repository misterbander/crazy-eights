package misterbander.crazyeights.scene2d

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import ktx.math.component1
import ktx.math.component2
import misterbander.crazyeights.scene2d.modules.Draggable
import misterbander.crazyeights.scene2d.modules.Rotatable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec
import kotlin.math.floor

fun GObject<*>.transformToGroupCoordinates(group: Group)
{
	getModule<SmoothMovable>()!!.apply {
		val dx = x - xInterpolator.value
		val dy = y - yInterpolator.value
		val dRotation = rotation - rotationInterpolator.value
		val (newX1, newY1) = localToActorCoordinates(group, tempVec.set(0F, 0F))
		val (newX2, newY2) = localToActorCoordinates(group, tempVec.set(1F, 0F))
		val rotation = rotation
		val newRotation = floor(MathUtils.atan2(newY2 - newY1, newX2 - newX1)*MathUtils.radDeg)
		x = newX1 + dx
		xInterpolator.value = newX1
		y = newY1 + dy
		yInterpolator.value = newY1
		this.rotation = newRotation + dRotation
		rotationInterpolator.value = newRotation
		getModule<Draggable>()!!.unrotatedDragPositionVec.rotateDeg(newRotation - rotation + dRotation)
		getModule<Rotatable>()!!.initialRotation += newRotation - rotation + dRotation
	}
}

fun getSpreadPositionForIndex(index: Int, spreadCardCount: Int, separation: Float, curvature: Float): Vector2
{
	val offsetFactor = -(spreadCardCount - 1)/2F + index
	return tempVec.set(offsetFactor*separation, -offsetFactor*offsetFactor*curvature)
}

fun getSpreadRotationForIndex(index: Int, spreadCardCount: Int, separation: Float, curvature: Float): Float =
	-getSpreadPositionForIndex(index, spreadCardCount, separation, curvature).x/25
