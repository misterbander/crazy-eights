package misterbander.crazyeights.net.packets

import com.badlogic.gdx.graphics.Color
import ktx.actors.plusAssign
import ktx.collections.*
import ktx.log.info
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerObject
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.CrazyEightsServer
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
		val opponentHand = this.userToHandMap.getOrPut(user.name) {
			OpponentHand(room)
		} as OpponentHand
		opponentHand.user = user
		this.opponentHands += opponentHand
	}
	if (!user.isAi)
		room.chatBox.chat("${user.name} joined the game", Color.YELLOW)
	this.arrangePlayers()
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

fun CrazyEightsServer.onSwapSeats(event: SwapSeatsEvent)
{
	val (user1, user2) = event
	val keys: GdxArray<String> = tabletop.hands.orderedKeys()
	val index1 = keys.indexOf(user1, false)
	val index2 = keys.indexOf(user2, false)
	keys.swap(index1, index2)
	server.sendToAllTCP(event)
	info("Server | INFO") { "$user1 swapped seats with $user2" }
}

object AiAddEvent

@NoArg
data class AiRemoveEvent(val username: String)

fun CrazyEightsServer.onAiAdd()
{
	aiCount++
	val name = aiNames.random() ?: "AI $aiCount"
	aiNames -= name
	val ai = User(name, Color.LIGHT_GRAY, true)
	tabletop.users[name] = ai
	tabletop.hands[name] = GdxArray()
	server.sendToAllTCP(UserJoinedEvent(ai))
}

fun CrazyEightsServer.onAiRemove(event: AiRemoveEvent)
{
	aiCount--
	val ai: User = tabletop.users.remove(event.username)
	val hand: GdxArray<ServerObject> = tabletop.hands.remove(event.username)
	for (card: ServerObject in hand)
		(card as ServerCard).setServerCardGroup(null, tabletop)
	if (!event.username.startsWith("AI "))
		aiNames += event.username
	server.sendToAllTCP(UserLeftEvent(ai))
}
