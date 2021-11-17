package misterbander.crazyeights.scene2d

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Group
import ktx.math.component1
import ktx.math.component2
import misterbander.crazyeights.scene2d.modules.Draggable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec

fun GObject<*>.transformToGroupCoordinates(group: Group)
{
	val smoothMovable = getModule<SmoothMovable>()!!
	val (newX1, newY1) = localToActorCoordinates(group, tempVec.set(0F, 0F))
	val (newX2, newY2) = localToActorCoordinates(group, tempVec.set(1F, 0F))
	val rotation = smoothMovable.rotationInterpolator.target
	val newRotation = MathUtils.atan2(newY2 - newY1, newX2 - newX1)*MathUtils.radDeg
	val deltaRotation = newRotation - rotation
	smoothMovable.setPositionAndTargetPosition(newX1, newY1)
	smoothMovable.rotationInterpolator.set(newRotation)
	getModule<Draggable>()!!.unrotatedDragPositionVec.rotateDeg(deltaRotation)
}
