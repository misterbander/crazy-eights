package misterbander.crazyeights.net.packets

import misterbander.crazyeights.model.NoArg

@NoArg
data class HandshakeReject(val reason: String)
