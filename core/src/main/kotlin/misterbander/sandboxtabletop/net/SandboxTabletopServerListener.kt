package misterbander.sandboxtabletop.net

import com.badlogic.gdx.utils.IntSet
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import ktx.collections.gdxSetOf
import ktx.collections.minusAssign
import ktx.collections.plusAssign
import ktx.log.info
import misterbander.sandboxtabletop.VERSION_STRING
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.net.packets.Handshake
import misterbander.sandboxtabletop.net.packets.HandshakeReject
import misterbander.sandboxtabletop.net.packets.RoomState
import java.util.UUID

class SandboxTabletopServerListener : Listener
{
	/** Contains ids of connections that have successfully performed a handshake. */
	private val handshookConnections = IntSet()
	
	private val users = gdxSetOf(User(UUID.randomUUID(), "bob")) //GdxSet<User>()
	
	override fun connected(connection: Connection)
	{
		connection.setName("Server-side client connection ${connection.id}")
	}
	
	override fun disconnected(connection: Connection)
	{
		handshookConnections.remove(connection.id)
		if (connection.arbitraryData is User)
			users -= connection.arbitraryData as User
	}
	
	override fun received(connection: Connection, `object`: Any)
	{
		val server = Network.server ?: return
		if (connection.id !in handshookConnections) // Connections must perform handshake before packets are processed
		{
			if (`object` is Handshake)
			{
				if (`object`.versionString != VERSION_STRING) // Version check
				{
					connection.sendTCP(HandshakeReject("Incorrect version! Your Sandbox Tabletop version is ${`object`.versionString}. Server version is $VERSION_STRING."))
					return
				}
				if (`object`.data?.size != 1) // Data integrity check
				{
					connection.sendTCP(HandshakeReject("Incorrect handshake data format! Expecting 1 argument but found ${`object`.data?.size}. This is a bug and shouldn't be happening, please notify the developer."))
					return
				}
				val username = `object`.data[0]
				if (users.any { it.username == username }) // Check username collision
				{
					connection.sendTCP(HandshakeReject("Username conflict! Username $username is already taken."))
					return
				}
				
				// Handshake is successful
				handshookConnections.add(connection.id)
				connection.sendTCP(Handshake(VERSION_STRING))
				info("Server | INFO") { "Successful handshake from $connection" }
			}
			else
				ktx.log.error("Server | ERROR") { "$connection attempted to send objects before handshake" }
			return
		}
		
		if (`object` is User) // A new user tries to join
		{
			connection.arbitraryData = `object`
			users += `object`
			connection.sendTCP(RoomState(users))
			info("Server | INFO") { "${`object`.username} (UUID: ${`object`.uuid}) joined the game" }
		}
	}
}
