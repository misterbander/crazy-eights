package misterbander.crazyeights.net.packets

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.esotericsoftware.kryonet.Connection
import kotlinx.coroutines.launch
import ktx.actors.along
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.async.KtxAsync
import ktx.collections.*
import ktx.log.info
import misterbander.crazyeights.game.ChangeSuitMove
import misterbander.crazyeights.game.DrawTwoEffectPenalty
import misterbander.crazyeights.game.draw
import misterbander.crazyeights.game.refillDrawStack
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.CardGroup
import misterbander.crazyeights.scene2d.EffectText
import misterbander.crazyeights.scene2d.Groupable
import misterbander.crazyeights.scene2d.PowerCardEffect
import misterbander.crazyeights.scene2d.PowerCardEffectRing
import misterbander.crazyeights.scene2d.SuitChooser
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.crazyeights.scene2d.actions.DrawAction
import misterbander.crazyeights.scene2d.actions.ShuffleAction
import misterbander.crazyeights.scene2d.modules.Ownable
import kotlin.math.min
import kotlin.math.round

@NoArg
data class EightsPlayedEvent(val playerUsername: String) : PowerCardPlayedEvent
{
	override val delayMillis: Long
		get() = 0
}

fun Tabletop.onEightsPlayed(event: EightsPlayedEvent)
{
	room.dramatic.play()
	val suitChooser = SuitChooser(room, event.playerUsername == game.user.name)
	this.suitChooser = suitChooser
	powerCardEffects.clearChildren()
	powerCardEffects += PowerCardEffect(room, discardPile!!.cards.peek() as Card) {
		targeting(powerLabelGroup, fadeOut(0.5F)) along Actions.run { powerCardEffects += suitChooser }
	}
	persistentPowerCardEffects += PowerCardEffectRing(room)
}

@NoArg
data class SuitDeclareEvent(val suit: ServerCard.Suit)

fun CrazyEightsServer.onSuitDeclare(connection: Connection? = null, event: SuitDeclareEvent)
{
	info("Server | INFO") { "Suit changed to ${event.suit.name}" }
	KtxAsync.launch {
		val serverGameState = serverGameState!!
		val topCard: ServerCard =
			(tabletop.idToObjectMap[tabletop.discardPileHolderId] as ServerCardHolder).cardGroup.cards.peek()
		kotlinx.coroutines.delay(1000)
		serverGameState.doMove(ChangeSuitMove(topCard, event.suit))
		server.sendToAllTCP(serverGameState.toGameState())
	}
	if (connection == null)
		server.sendToAllTCP(event)
	else
		server.sendToAllExceptTCP(connection.id, event)
}

@NoArg
data class DrawTwosPlayedEvent(val drawCardCount: Int) : PowerCardPlayedEvent
{
	override val delayMillis: Long
		get() = 2000
}

fun Tabletop.onDrawTwosPlayed(packet: DrawTwosPlayedEvent)
{
	powerCardEffects.clearChildren()
	powerCardEffects += PowerCardEffect(room, discardPile!!.cards.peek() as Card) {
		defaultAction along Actions.run {
			powerCardEffects += EffectText(room, "+${packet.drawCardCount}")
		}
	}
	persistentPowerCardEffects += PowerCardEffectRing(room)
}

@NoArg
data class DrawTwoPenaltyEvent(val victimUsername: String, val drawCardCount: Int)

fun Tabletop.onDrawTwoPenalty(event: DrawTwoPenaltyEvent)
{
	val (victimUsername, drawCardCount) = event
	val drawStackHolder = drawStackHolder!!
	val hand = userToHandMap[victimUsername]!!
	val drawTwoEffectText = powerCardEffects.children.firstOrNull { it is EffectText } as? EffectText
	drawTwoEffectText?.moveToHand(hand)
	
	drawStack!! += Actions.run {
		drawStackHolder.isFlashing = false
		drawStackHolder.touchable = Touchable.disabled
		myHand.touchable = Touchable.disabled
	} then delay(1.5F, DrawAction(room, hand, drawCardCount)) then Actions.run {
		drawStackHolder.touchable = Touchable.enabled
		myHand.touchable = Touchable.enabled
		game.client?.sendTCP(ActionLockReleaseEvent)
	}
}

fun CrazyEightsServer.acceptDrawTwoPenalty(acceptorUsername: String)
{
	val serverGameState = serverGameState!!
	val drawStack = (tabletop.idToObjectMap[tabletop.drawStackHolderId] as ServerCardHolder).cardGroup
	
	KtxAsync.launch {
		if (drawStack.cards.size < serverGameState.drawTwoEffectCardCount)
			refillDrawStack()
		waitForActionLocks()
		
		acquireActionLocks()
		val drawCount = min(drawStack.cards.size, serverGameState.drawTwoEffectCardCount)
		repeat(drawCount) { draw(drawStack.cards.peek(), acceptorUsername) }
		server.sendToAllTCP(DrawTwoPenaltyEvent(acceptorUsername, drawCount))
		lastPowerCardPlayedEvent = null
		
		waitForActionLocks()
		serverGameState.doMove(DrawTwoEffectPenalty(drawCount))
		server.sendToAllTCP(serverGameState.toGameState())
	}
}

@NoArg
data class SkipsPlayedEvent(val victimUsername: String) : PowerCardPlayedEvent
{
	override val delayMillis: Long
		get() = 2500
}

fun Tabletop.onSkipsPlayed(event: SkipsPlayedEvent)
{
	powerCardEffects.clearChildren()
	powerCardEffects += PowerCardEffect(room, discardPile!!.cards.peek() as Card) {
		defaultAction along Actions.run {
			powerCardEffects += EffectText(room, "Q", userToHandMap[event.victimUsername]!!)
		}
	}
	persistentPowerCardEffects += PowerCardEffectRing(room)
//	isPowerCardJustPlayed = true
}

object ReversePlayedEvent : PowerCardPlayedEvent
{
	override val delayMillis: Long
		get() = 2000
}

fun Tabletop.onReversePlayed()
{
	powerCardEffects.clearChildren()
	powerCardEffects += PowerCardEffect(room, discardPile!!.cards.peek() as Card) {
		defaultAction along Actions.run {
			persistentPowerCardEffects += PowerCardEffectRing(room)
			room.deepWhoosh.play()
			playDirectionIndicator.flipDirection()
		}
	}
	persistentPowerCardEffects += PowerCardEffectRing(room)
}

data class DrawStackRefillEvent(val cardGroupChangeEvent: CardGroupChangeEvent? = null, val shuffleSeed: Long = 0)

fun Tabletop.onDrawStackRefill(event: DrawStackRefillEvent)
{
	val (cardGroupChangeEvent, shuffleSeed) = event
	val drawStack = drawStack!!
	val discardPile = discardPile!!
	val discards = GdxArray(discardPile.cards)
	val topCard: Groupable<CardGroup> = discards.pop()
	
	for (discard: Groupable<CardGroup> in discards) // Unlock and disown everything in discards
	{
		discard.lockable.unlock(false)
		discard.getModule<Ownable>()?.wasInHand = false
	}
	
	onCardGroupChange(cardGroupChangeEvent!!)
	drawStack.flip(false)
	
	topCard.smoothMovable.apply {
		x = 0F
		y = 0F
		rotation = 180*round(rotation/180)
	}
	
	val prevDrawStackTouchable = drawStackHolder!!.touchable
	val prevMyHandTouchable = myHand.touchable
	drawStack += Actions.run {
		drawStackHolder!!.touchable = Touchable.disabled
		myHand.touchable = Touchable.disabled
	} then
		delay(0.5F, ShuffleAction(room, shuffleSeed)) then
		Actions.run {
			drawStackHolder!!.touchable = prevDrawStackTouchable
			myHand.touchable = prevMyHandTouchable
			game.client?.sendTCP(ActionLockReleaseEvent)
		}
}
