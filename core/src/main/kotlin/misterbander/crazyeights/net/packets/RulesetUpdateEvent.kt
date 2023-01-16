package misterbander.crazyeights.net.packets

import misterbander.crazyeights.net.server.game.Ruleset

@NoArg
data class RulesetUpdateEvent(val ruleset: Ruleset, val changerUsername: String = "")
