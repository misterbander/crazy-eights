package misterbander.gframework.scene2d

import com.badlogic.gdx.physics.box2d.Fixture

/**
 * Implement this to handle `Box2D` collisions. For this to work, the [GObject] must be added to the `Box2D` body
 * as user data.
 */
interface GContactListener
{
	/**
	 * Called when this GObject begins contact with another fixture.
	 * @param otherFixture the other fixture in contact
	 */
	fun beginContact(otherFixture: Fixture) {}
	
	/**
	 * Called when this GObject stops contact with another fixture.
	 * @param otherFixture the other fixture in contact
	 */
	fun endContact(otherFixture: Fixture) {}
}
