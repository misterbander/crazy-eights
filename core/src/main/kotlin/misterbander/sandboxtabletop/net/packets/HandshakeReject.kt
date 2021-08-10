package misterbander.sandboxtabletop.net.packets

data class HandshakeReject(val reason: String)
{
	@Suppress("UNUSED")
	private constructor() : this("")
}
