package misterbander.crazyeights.net.packets

import com.badlogic.gdx.graphics.Color
import ktx.actors.plusAssign
import ktx.collections.*
import ktx.log.info
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerObject
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.ServerTabletop
import misterbander.crazyeights.scene2d.OpponentHand
import misterbander.crazyeights.scene2d.Tabletop

@NoArg
data class UserJoinedEvent(val user: User)

@NoArg
data class UserLeftEvent(val user: User)

fun Tabletop.onUserJoined(event: UserJoinedEvent)
{
	val user = event.user
	if (user != game.user)
	{
		this += user
		val opponentHand = userToHandMap.getOrPut(user.name) {
			OpponentHand(room)
		} as OpponentHand
		opponentHand.user = user
		opponentHands += opponentHand
	}
	if (!user.isAi)
		room.chatBox.chat("${user.name} joined the game", Color.YELLOW)
	arrangePlayers()
}

fun Tabletop.onUserLeft(event: UserLeftEvent)
{
	val user = event.user
	this -= user
	if (!user.isAi)
		room.chatBox.chat("${user.name} left the game", Color.YELLOW)
	arrangePlayers()
	if (user == room.userDialog.user)
		room.userDialog.hide()
}

@NoArg
data class SwapSeatsEvent(val username1: String, val username2: String)

fun Tabletop.onSwapSeats(event: SwapSeatsEvent)
{
	val (user1, user2) = event
	val keys: GdxArray<String> = userToHandMap.orderedKeys()
	val index1 = keys.indexOf(user1, false)
	val index2 = keys.indexOf(user2, false)
	keys.swap(index1, index2)
	arrangePlayers()
}

fun ServerTabletop.onSwapSeats(event: SwapSeatsEvent)
{
	val (user1, user2) = event
	val keys: GdxArray<String> = hands.orderedKeys()
	val index1 = keys.indexOf(user1, false)
	val index2 = keys.indexOf(user2, false)
	keys.swap(index1, index2)
	parent.server.sendToAllTCP(event)
	info("Server | INFO") { "$user1 swapped seats with $user2" }
}

object AiAddEvent

@NoArg
data class AiRemoveEvent(val username: String)

fun ServerTabletop.onAiAdd()
{
	if (parent.aiCount >= 6)
		return
	parent.aiCount++
	val name = parent.aiNames.random() ?: "AI ${parent.aiCount}"
	parent.aiNames -= name
	val ai = User(name, Color.LIGHT_GRAY, true)
	users[name] = ai
	hands[name] = GdxArray()
	parent.server.sendToAllTCP(UserJoinedEvent(ai))
}

fun ServerTabletop.onAiRemove(event: AiRemoveEvent)
{
	parent.aiCount--
	val ai: User = users.remove(event.username)
	val hand: GdxArray<ServerObject> = hands.remove(event.username)
	for (card: ServerObject in hand)
		(card as ServerCard).setServerCardGroup(this, null)
	if (!event.username.startsWith("AI "))
		parent.aiNames += event.username
	parent.server.sendToAllTCP(UserLeftEvent(ai))
}
