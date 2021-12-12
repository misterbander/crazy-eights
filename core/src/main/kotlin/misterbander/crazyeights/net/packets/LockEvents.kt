package misterbander.crazyeights.net.packets

import com.badlogic.gdx.utils.IntMap
import ktx.collections.*
import ktx.log.debug
import misterbander.crazyeights.game.DrawMove
import misterbander.crazyeights.game.draw
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerLockable
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.crazyeights.scene2d.modules.Lockable

@NoArg
data class ObjectLockEvent(val id: Int, val lockerUsername: String)

@NoArg
data class ObjectUnlockEvent(val id: Int, val unlockerUsername: String, val sideEffects: Boolean = true)

fun Tabletop.onObjectLock(event: ObjectLockEvent)
{
	val (id, lockerUsername) = event
	val toLock = idToGObjectMap[id]!!
	toLock.getModule<Lockable>()?.lock(users[lockerUsername]!!)
	
	if (room.isGameStarted && toLock is Card && toLock.cardGroup == drawStack)
		room.gameState!!.drawCount++
}

fun CrazyEightsServer.onObjectLock(event: ObjectLockEvent)
{
	if (actionLocks.isNotEmpty())
		return
	val (id, lockerUsername) = event
	val toLock = tabletop.idToObjectMap[id]!!
	if (toLock !is ServerLockable || !toLock.canLock) // Only unlocked draggables can be locked
		return
	if (isGameStarted)
	{
		val serverGameState = serverGameState!!
		if (lockerUsername != serverGameState.currentPlayer.name) // You can't lock anything if it's not your turn
			return
		if (toLock is ServerCard && toLock.cardGroupId != -1)
		{
			val cardGroup = tabletop.idToObjectMap[toLock.cardGroupId] as ServerCardGroup
			if (cardGroup.cardHolderId == tabletop.drawStackHolderId)
			{
				if (serverGameState.drawCount >= 3)
					return
				if (serverGameState.drawTwoEffectCardCount > 0)
				{
					acceptDrawTwoPenalty(lockerUsername)
					return
				}
				serverGameState.doMove(DrawMove)
			}
			else if (cardGroup.cardHolderId == tabletop.discardPileHolderId) // Discard pile cannot be locked
				return
		}
		else if (toLock is ServerCardGroup) // Card groups can't be locked during games
			return
	}
	debug("Server | DEBUG") { "$lockerUsername locks $toLock" }
	toLock.lockHolder = lockerUsername
	toLock.toFront(tabletop)
	server.sendToAllTCP(event)
}

fun CrazyEightsServer.onObjectUnlock(event: ObjectUnlockEvent)
{
	val (id, unlockerUsername, sideEffects) = event
	val toUnlock = tabletop.idToObjectMap[id] ?: return
	if (toUnlock !is ServerLockable || toUnlock.lockHolder != unlockerUsername)
		return
	debug("Server | DEBUG") { "${toUnlock.lockHolder} unlocks $toUnlock" }
	toUnlock.lockHolder = null
	if (toUnlock is ServerCard)
	{
		if (toUnlock.cardGroupId != -1)
		{
			val cardGroup = tabletop.idToObjectMap[toUnlock.cardGroupId] as ServerCardGroup
			if (isGameStarted && cardGroup.cardHolderId == tabletop.drawStackHolderId)
			{
				server.sendToAllTCP(event)
				draw(cardGroup.cards.peek(), unlockerUsername, fireOwnEvent = true, playSound = true)
				return
			}
			else
				cardGroup.arrange()
		}
		else if (isGameStarted)
		{
			// If the card is left on the table without being in someone's hand or in a card group, then it will be
			// returned to its original owner
			toUnlock.lockHolder = ""
			runLater.getOrPut(unlockerUsername) { IntMap() }.put(
				toUnlock.id,
				CrazyEightsServer.CancellableRunnable(
					runnable = {
						toUnlock.lockHolder = null
						draw(toUnlock, unlockerUsername, fireOwnEvent = true)
					},
					onCancel = { toUnlock.lockHolder = null }
				)
			)
		}
		if (!toUnlock.justMoved && !toUnlock.justRotated && sideEffects && !isGameStarted)
		{
			toUnlock.isFaceUp = !toUnlock.isFaceUp
			server.sendToAllTCP(CardFlipEvent(id))
		}
		toUnlock.justMoved = false
		toUnlock.justRotated = false
	}
	else if (toUnlock is ServerCardGroup)
	{
		if (toUnlock.cardHolderId != -1)
			toUnlock.rotation = 0F
	}
	server.sendToAllTCP(event)
}
