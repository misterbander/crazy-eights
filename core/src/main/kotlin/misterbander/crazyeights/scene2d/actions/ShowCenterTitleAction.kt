package misterbander.crazyeights.scene2d.actions

import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import ktx.actors.plusAssign
import ktx.actors.txt
import misterbander.crazyeights.RoomScreen

class ShowCenterTitleAction(
	private val room: RoomScreen,
	private val title: String,
	private val duration: Float = -1F
) : RunnableAction()
{
	override fun run()
	{
		room.centerTitleContainer.isVisible = true
		room.centerTitle.txt = title
		if (duration > 0)
			room.stage += delay(duration, HideCenterTitleAction(room))
	}
}
