package misterbander.gframework.scene2d.module

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import ktx.math.component1
import ktx.math.component2
import misterbander.gframework.GFramework
import misterbander.gframework.scene2d.GObject
import kotlin.math.sqrt

/**
 * Module that simplifies `Box2D` movement to intuitive mathematical properties like direction and speed.
 *
 * The `Box2D` of the [GObject] must not be null.
 * @param parent the parent `GObject` this module is attached to
 */
open class SimpleMovement2D<T : GFramework>(parent: GObject<T>) : GModule<T>(parent)
{
	private val body by lazy { parent.body!! }
	
	private var isGoingBackwards = false
	var prevX: Float = 0F
	var prevY: Float = 0F
	
	override fun update(delta: Float)
	{
		val x = body.position.x/parent.screen.mpp
		val y = body.position.y/parent.screen.mpp
		if (prevX != x || prevY != y)
		{
			parent.setPosition(x, y)
			prevX = x
			prevY = y
		}
	}
	
	/**
	 * The direction the body is travelling, in degrees. Ranges between (-180, 180] where 0 is to the right and increases
	 * anti-clockwise.
	 */
	var direction: Float = 0F
		get()
		{
			if (speed2 > 0F)
				field = body.linearVelocity.angle(Vector2.X)
			if (isGoingBackwards)
			{
				if (field > 0F)
					field -= 180F
				else
					field += 180F
			}
			return field
		}
		set(value)
		{
			field = value
			setVelocity(speed*MathUtils.cosDeg(field), speed*MathUtils.sinDeg(field))
		}
	
	/**
	 * The speed this body is travelling, in pixels per second. Negative means backwards.
	 */
	var speed: Float
		get()
		{
			val speed = sqrt(hspeed*hspeed + vspeed*vspeed)
			return if (isGoingBackwards) -speed else speed
		}
		set(value)
		{
			isGoingBackwards = value < 0
			val vx = value*MathUtils.cosDeg(direction)
			val vy = value*MathUtils.sinDeg(direction) // Final velocity
			setVelocity(vx, vy)
		}
	
	val speed2: Float
		get() = hspeed*hspeed + vspeed*vspeed
	
	/**
	 * The horizontal speed of this body in pixels per second. Positive means to the right.
	 */
	var hspeed: Float
		get() = body.linearVelocity.x/parent.screen.mpp
		set(value) = setVelocity(value, vspeed)
	
	/**
	 * The vertical speed of this body in pixels per second. Positive means upwards.
	 */
	var vspeed: Float
		get() = body.linearVelocity.y/parent.screen.mpp
		set(value) = setVelocity(hspeed, value)
	
	/**
	 * Sets the velocity of the body, in pixels per second.
	 * @param vx the horizontal velocity
	 * @param vy the vertical velocity
	 */
	fun setVelocity(vx: Float, vy: Float)
	{
		if (body.type == BodyDef.BodyType.DynamicBody)
		{
			// Change velocity by applying impulse
			val (ux, uy) = body.linearVelocity // Initial velocity
			// Calculate change in momentum, or impulse
			val dMomentumX: Float = body.mass*(vx*parent.screen.mpp - ux)
			val dMomentumY: Float = body.mass*(vy*parent.screen.mpp - uy)
			body.applyLinearImpulse(dMomentumX, dMomentumY, body.worldCenter.x, body.worldCenter.y, true)
		}
		else
			body.setLinearVelocity(vx, vy)
	}
}
