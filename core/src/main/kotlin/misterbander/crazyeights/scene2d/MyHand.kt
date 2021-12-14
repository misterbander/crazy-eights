package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.Align
import ktx.collections.*
import ktx.math.component1
import ktx.math.component2
import ktx.scene2d.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.INFO_LABEL_STYLE_S
import misterbander.crazyeights.Room
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.net.packets.HandUpdateEvent
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec

class MyHand(private val room: Room) : Hand(room), DragTarget
{
	val offsetCenterY = 48F
	override val cardGroup = CardGroup(room, y = offsetCenterY, type = ServerCardGroup.Type.SPREAD)
	private val spectatorLabel = scene2d.label("You are a spectator. Please wait for a new game.", INFO_LABEL_STYLE_S) {
		setPosition(0F, offsetCenterY, Align.bottom)
	}
	
	private val memory = GdxSet<Card>()
	
	init
	{
		addActor(cardGroup)
		addActor(spectatorLabel)
		room.addUprightGObject(this)
	}
	
	override fun update(delta: Float)
	{
		val (handX, handY) = stage.screenToStageCoordinates(tempVec.set(Gdx.graphics.width.toFloat()/2, Gdx.graphics.height.toFloat()))
		setPosition(handX, handY)
		spectatorLabel.isVisible = room.isGameStarted && game.user.name !in room.gameState!!.players
	}
	
	override fun canAccept(gObject: GObject<CrazyEights>): Boolean = gObject is Card || gObject is CardGroup
	
	override fun accept(gObject: GObject<CrazyEights>)
	{
		if (gObject is Card)
		{
			arrange()
			if (!gObject.ownable.wasInHand)
			{
				gObject.ownable.wasInHand = true
				gObject.isFaceUp = true
				sendUpdates()
			}
		}
		else if (gObject is CardGroup)
		{
			val insertIndex = cardGroup.cards.indexOf(gObject, true)
			cardGroup -= gObject
			val cards: Array<Card> = gObject.cards.toArray(Card::class.java)
			cards.forEachIndexed { index, card ->
				card.ownable.wasInHand = true
				card.isFaceUp = true
				cardGroup.insert(card, insertIndex + index)
			}
			arrange(false)
			gObject.remove()
			sendUpdates()
		}
	}
	
	override fun arrange(sort: Boolean)
	{
		val defaultSeparation = 100F
		val max = room.uiViewport.worldWidth - 200
		cardGroup.spreadSeparation =
			if ((cardGroup.cards.size - 1)*defaultSeparation < max) defaultSeparation else max/(cardGroup.cards.size - 1)
		cardGroup.arrange(sort)
		
		if (room.isGameStarted)
		{
			room.apply {
				if (gameState!!.drawCount >= gameState!!.ruleset.maxDrawCount && gameState!!.currentPlayer == game.user.name)
				{
					passButton.isVisible = true
					tabletop.drawStackHolder!!.touchable = Touchable.disabled
				}
			}
			rememberCards()
		}
	}
	
	fun setDarkened(predicate: (Card) -> Boolean)
	{
		for (card in memory.toGdxArray())
		{
			if (card.cardGroup == room.tabletop.discardPile)
			{
				card.isDarkened = false
				memory -= card
			}
			else
				card.isDarkened = predicate(card)
		}
	}
	
	private fun rememberCards()
	{
		if (!room.isGameStarted)
			return
		cardGroup.cards.forEach { memory += it as Card }
	}
	
	fun clearMemory() = memory.clear()
	
	fun sendUpdates()
	{
		game.client?.apply {
			outgoingPacketBuffer += HandUpdateEvent(cardGroup.cards.map { (it as Card).toServerCard() }, game.user.name)
		}
	}
	
	fun reset()
	{
		cardGroup.clearChildren()
		touchable = Touchable.enabled
	}
}
