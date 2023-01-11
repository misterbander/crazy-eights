package misterbander.gframework.scene2d

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage

fun Stage.fixedAct() = root.fixedAct()

/**
 * Recursively calls [fixedAct] in the group's children.
 */
fun Group.fixedAct()
{
	val actors: Array<out Actor> = children.begin()
	for (actor in actors)
	{
		if (actor is Group)
		{
			if (actor is GObject<*>)
				actor.fixedAct()
			actor.fixedAct()
		}
	}
	children.end()
}
