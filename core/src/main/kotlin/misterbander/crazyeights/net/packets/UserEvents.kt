package misterbander.crazyeights.net.packets

import com.badlogic.gdx.graphics.Color
import ktx.actors.plusAssign
import ktx.collections.*
import ktx.log.info
import misterbander.crazyeights.Room
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.scene2d.OpponentHand

@NoArg
data class UserJoinedEvent(val user: User)

@NoArg
data class UserLeftEvent(val user: User)

fun Room.onUserJoined(event: UserJoinedEvent)
{
	val user = event.user
	if (user != game.user)
	{
		tabletop += user
		val opponentHand = tabletop.userToHandMap.getOrPut(user.name) {
			OpponentHand(this)
		} as OpponentHand
		opponentHand.user = user
		tabletop.opponentHands += opponentHand
	}
	if (!user.isAi)
		chatBox.chat("${user.name} joined the game", Color.YELLOW)
	tabletop.arrangePlayers()
}

fun Room.onUserLeft(event: UserLeftEvent)
{
	val user = event.user
	tabletop -= user
	if (!user.isAi)
		chatBox.chat("${user.name} left the game", Color.YELLOW)
	tabletop.arrangePlayers()
	if (user == userDialog.user)
		userDialog.hide()
}

@NoArg
data class SwapSeatsEvent(val username1: String, val username2: String)

fun Room.onSwapSeats(event: SwapSeatsEvent)
{
	val (user1, user2) = event
	val keys: GdxArray<String> = tabletop.userToHandMap.orderedKeys()
	val index1 = keys.indexOf(user1, false)
	val index2 = keys.indexOf(user2, false)
	keys.swap(index1, index2)
	tabletop.arrangePlayers()
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
	val ai = tabletop.users.remove(event.username)
	tabletop.hands.remove(event.username)
	if (!event.username.startsWith("AI "))
		aiNames += event.username
	server.sendToAllTCP(UserLeftEvent(ai))
}
