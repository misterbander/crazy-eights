package misterbander.crazyeights.net.packets

import ktx.collections.*
import misterbander.crazyeights.net.server.ServerCard

@NoArg
data class CardGroupCreateEvent(val id: Int = -1, val cards: GdxArray<ServerCard>)

@NoArg
data class CardGroupChangeEvent(val cards: GdxArray<ServerCard>, val newCardGroupId: Int, val changerUsername: String)

@NoArg
data class CardGroupDetachEvent(val cardHolderId: Int, val replacementCardGroupId: Int = -1, val changerUsername: String)

@NoArg
data class CardGroupDismantleEvent(val id: Int)

@NoArg
data class CardGroupShuffleEvent(val id: Int, val seed: Long)
