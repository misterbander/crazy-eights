package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import ktx.actors.txt
import ktx.collections.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.gframework.scene2d.GObject

class Debug(private val room: RoomScreen) : GObject<CrazyEights>(room)
{
	private val lockedObjects = GdxArray<GObject<*>>()
	
	init
	{
		isVisible = false
	}
	
	override fun update(delta: Float)
	{
		if (Gdx.input.isKeyJustPressed(Input.Keys.F3))
			isVisible = !isVisible
		if (!isVisible)
			return
		
		val serverObjectsDebugString = game.server?.tabletop?.serverObjectsDebugString ?: ""
		val handsDebugString = game.server?.tabletop?.handsDebugString ?: ""
		
		// View all objects locked by the user
		lockedObjects.clear()
		for (actor: Actor in room.tabletop.cards.children)
		{
			if ((actor as? GObject<*>)?.getModule<Lockable>()?.isLockHolder == true)
				lockedObjects += actor
		}
		val lockedObjectsDebug = "Locked objects (${lockedObjects.size}):\n$lockedObjects"
		room.debugInfo.txt = "Scroll focus = ${room.stage.scrollFocus}\n" +
			"$serverObjectsDebugString\n$handsDebugString\n$lockedObjectsDebug"
	}
	
	override fun setVisible(visible: Boolean)
	{
		super.setVisible(visible)
		room.debugInfo.isVisible = visible
	}
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		val shapeDrawer = game.shapeDrawer
		shapeDrawer.rectangle(2F, 2F, (1280 - 4).toFloat(), (720 - 4).toFloat(), Color.GREEN)
	}
}
