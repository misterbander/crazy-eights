package misterbander.crazyeights.scene2d.actions

import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import misterbander.crazyeights.RoomScreen

class HideCenterTitleAction(private val room: RoomScreen) : RunnableAction()
{
	override fun run()
	{
		room.centerTitleContainer.isVisible = false
	}
}
