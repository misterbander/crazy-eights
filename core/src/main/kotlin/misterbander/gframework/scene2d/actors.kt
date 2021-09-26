package misterbander.gframework.scene2d

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import ktx.actors.plusAssign
import misterbander.gframework.GFramework

/**
 * Spawns the [GObject] into the world by adding it to the stage. Calls `onSpawn` on the [GObject] and its modules.
 * @param gObject the [GObject] to spawn
 */
operator fun <T : GFramework> Stage.plusAssign(gObject: GObject<T>)
{
	this += gObject
	gObject.onSpawnInternal()
}

/**
 * Spawns the [GObject] into the world as a child of the group. Calls `onSpawn` on the [GObject] and its modules.
 * @param gObject the [GObject] to spawn
 */
operator fun <T : GFramework> Group.plusAssign(gObject: GObject<T>)
{
	this += gObject
	gObject.onSpawnInternal()
}
