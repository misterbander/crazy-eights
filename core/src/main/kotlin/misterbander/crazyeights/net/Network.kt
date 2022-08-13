package misterbander.crazyeights.net

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.log.info

class Network
{
	private val asyncContext = newSingleThreadAsyncContext("Network-AsyncExecutor-Thread")
	var server: CrazyEightsServer? = null
		private set
	var client: CrazyEightsClient? = null
		private set
	private var stopNetworkJob: Job? = null
	
	suspend fun createAndStartServer(roomCode: String, port: Int): CrazyEightsServer
	{
		stopNetworkJob?.join()
		info("Network | INFO") { "Starting server on port $port..." }
		// Create the server in a separate thread to avoid nasty lag spike
		server = CrazyEightsServer(roomCode)
		withContext(asyncContext) { server!!.start(port) }
		info("Network | INFO") { "Started server on port $port" }
		return server!!
	}
	
	suspend fun createAndConnectClient(ip: String, port: Int): CrazyEightsClient
	{
		stopNetworkJob?.join()
		info("Network | INFO") { "Connecting to $ip on port $port" }
		client = CrazyEightsClient()
		withContext(asyncContext) { client!!.connect(ip, port) }
		return client!!
	}
	
	suspend fun createAndConnectClientByRoomCode(roomCode: String): CrazyEightsClient
	{
		stopNetworkJob?.join()
		client = CrazyEightsClient()
		withContext(asyncContext) { client!!.discoverHostByRoomCode(roomCode) }
		return client!!
	}
	
	fun stop()
	{
		stopNetworkJob = KtxAsync.launch {
			val stopServerJob = server?.stop()
			client?.stop()
			stopServerJob?.join()
			info("Network | INFO") { "Network stopped" }
			server = null
			client = null
		}
	}
}
