package misterbander.crazyeights.net.packets

import misterbander.crazyeights.model.NoArg

@NoArg
data class TouchUpEvent(val username: String, val pointer: Int)
