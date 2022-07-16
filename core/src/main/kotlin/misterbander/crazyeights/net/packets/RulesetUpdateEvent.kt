package misterbander.crazyeights.net.packets

import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.game.Ruleset
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.net.CrazyEightsServer

@NoArg
data class RulesetUpdateEvent(val ruleset: Ruleset, val changerUsername: String = "")

fun RoomScreen.onRulesetUpdate(event: RulesetUpdateEvent)
{
	val (ruleset, changerUsername) = event
	if (changerUsername.isNotEmpty())
	{
		when
		{
			ruleset.maxDrawCount != this.ruleset.maxDrawCount ->
				chatBox.chat("$changerUsername set Max Draw Count to ${if (ruleset.maxDrawCount == Int.MAX_VALUE) 0 else ruleset.maxDrawCount}")
			ruleset.drawTwos != this.ruleset.drawTwos ->
				chatBox.chat("$changerUsername set Draw Twos to ${ruleset.drawTwos ?: "Off"}")
			ruleset.skips != this.ruleset.skips ->
				chatBox.chat("$changerUsername set Skips to ${ruleset.skips ?: "Off"}")
			ruleset.reverses != this.ruleset.reverses ->
				chatBox.chat("$changerUsername set Reverses to ${ruleset.reverses ?: "Off"}")
		}
	}
	this.ruleset = ruleset
	gameSettingsDialog.updateRuleset(ruleset)
}

fun CrazyEightsServer.onRulesetUpdate(event: RulesetUpdateEvent)
{
	if (isGameStarted)
		return
	ruleset = event.ruleset
	server.sendToAllTCP(event)
}
