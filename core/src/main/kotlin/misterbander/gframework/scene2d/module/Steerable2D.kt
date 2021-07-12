package misterbander.gframework.scene2d.module

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.World
import ktx.box2d.rayCast
import ktx.math.component1
import ktx.math.component2
import ktx.math.vec2
import misterbander.gframework.GFramework
import misterbander.gframework.scene2d.GObject

/**
 * Module that allows GObjects to move to a target while trying to avoid obstacles. It does this by attempting to
 * steer around the obstacle when it finds one via ray tracing. This does not guarantee that a path will always be
 * found even if one exists nor the route will be the shortest.
 * @param parent the parent GObject this module is attached to
 * @property steeringSpeed how fast the body should move to the target, in pixels per second
 * @property visionLength how far the body ray traces for an obstacle
 * @property isObstacle function that determines whether a fixture is an obstacle
 */
class Steerable2D<T : GFramework>(
	parent: GObject<T>,
	var steeringSpeed: Float = 1F,
	var visionLength: Float = 1F,
	val isObstacle: (Fixture) -> Boolean,
	val willDirectionHit: (Float) -> Boolean
) : SimpleMovement2D<T>(parent)
{
	private val world: World = parent.screen.world!!
	val target: Vector2 = vec2()
	private val targetVector = vec2()
	private var mode = SteeringMode.NOT_AVOIDING
	private var searchAngleDeviate: Float = 0F
	
	override fun update(delta: Float)
	{
		// Scale down to box2D scale
		val (x, y) = parent.body!!.position
		val tx = target.x*parent.screen.mpp
		val ty = target.y*parent.screen.mpp
		val visionLength = this.visionLength*parent.screen.mpp
		
		targetVector.set(tx - x, ty - y)
		val targetDir = targetVector.angle() // Direction vector to target
		
		if (mode == SteeringMode.NOT_AVOIDING)
		{
			// Is there something in the way?
			if (willDirectionHit(targetDir))
			{
				mode = SteeringMode.SEARCHING
				speed = 0F
			}
			else // Go straight ahead towards target
			{
				direction = targetDir
				speed = steeringSpeed
			}
		}
		else if (mode == SteeringMode.SEARCHING) // Search for walkable directions
		{
			searchAngleDeviate += 9F
			val leftDir = direction + searchAngleDeviate
			val rightDir = direction - searchAngleDeviate
				
			if (!willDirectionHit(leftDir))
			{
				mode = SteeringMode.STEERING_LEFT
				direction = leftDir
				searchAngleDeviate = 0F
			}
			else if (!willDirectionHit(rightDir))
			{
				mode = SteeringMode.STEERING_RIGHT
				direction = rightDir
				searchAngleDeviate = 0F
			}
		}
		else if (mode == SteeringMode.STEERING_LEFT)
		{
		
		}
		
		super.update(delta)
	}
	
	private fun rayCast(x1: Float, y1: Float, x2: Float, y2: Float): Boolean
	{
		var hit = false
		world.rayCast(x1, y1, x2, y2)
		{ fixture: Fixture, _: Vector2, _: Vector2, _: Float ->
			if (isObstacle(fixture))
			{
				hit = true
				0F
			}
			else 1F
		}
		return hit
	}
	
	enum class SteeringMode
	{
		NOT_AVOIDING, SEARCHING, STEERING_LEFT, STEERING_RIGHT
	}
}