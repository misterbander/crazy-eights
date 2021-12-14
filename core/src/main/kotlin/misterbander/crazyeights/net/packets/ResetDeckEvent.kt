package misterbander.crazyeights.net.packets

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.collections.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.game.resetDeck
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.crazyeights.scene2d.actions.ShuffleAction
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.Ownable
import misterbander.gframework.scene2d.GObject

data class ResetDeckEvent(val cardGroupChangeEvent: CardGroupChangeEvent? = null, val shuffleSeed: Long = 0)

fun Tabletop.onResetDeck(event: ResetDeckEvent)
{
	val (cardGroupChangeEvent, shuffleSeed) = event
	for (gObject: GObject<CrazyEights> in idToGObjectMap.values()) // Unlock and disown everything
	{
		gObject.getModule<Lockable>()?.unlock(false)
		gObject.getModule<Ownable>()?.wasInHand = false
	}
	onCardGroupChange(cardGroupChangeEvent!!)
	val drawStack = drawStack!!
	drawStack.flip(false)
	drawStack += Actions.run {
		drawStackHolder!!.touchable = Touchable.disabled
		myHand.touchable = Touchable.disabled
	} then
		Actions.delay(0.5F, ShuffleAction(room, shuffleSeed)) then
		Actions.run {
			drawStackHolder!!.touchable = Touchable.enabled
			myHand.touchable = Touchable.enabled
			game.client?.sendTCP(ActionLockReleaseEvent)
		}
}

fun CrazyEightsServer.onResetDeck()
{
	if (actionLocks.isNotEmpty())
		return
	val seed = MathUtils.random.nextLong()
	val cardGroupChangeEvent = resetDeck(seed, false)
	acquireActionLocks()
	server.sendToAllTCP(ResetDeckEvent(cardGroupChangeEvent, seed))
}
