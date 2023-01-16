package misterbander.crazyeights.net

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import ktx.async.KtxAsync
import misterbander.crazyeights.net.client.CrazyEightsClient
import misterbander.crazyeights.net.server.CrazyEightsServer

class Net
{
	var server: CrazyEightsServer? = null
		private set
	var client: CrazyEightsClient? = null
		private set
	private var stopNetworkJob: Job? = null
	
	suspend fun createAndStartServer(roomCode: String, port: Int): CrazyEightsServer
	{
		stopNetworkJob?.join()
		server = CrazyEightsServer(roomCode)
		// Start the server in a separate thread to avoid nasty lag spike
		server!!.start(port)
		yield()
		return server!!
	}
	
	suspend fun createAndConnectClient(
		hostAddress: String,
		port: Int,
		timeout: Int = 3000,
		retryInterval: Long = 3000,
		maxRetries: Int = Int.MAX_VALUE
	): CrazyEightsClient
	{
		stopNetworkJob?.join()
		client = CrazyEightsClient()
		client!!.connect(hostAddress, port, timeout, retryInterval, maxRetries)
		yield()
		return client!!
	}
	
	suspend fun createAndConnectClientByRoomCode(roomCode: String): CrazyEightsClient
	{
		stopNetworkJob?.join()
		client = CrazyEightsClient()
		client!!.discoverHostByRoomCode(roomCode)
		return client!!
	}
	
	fun stop()
	{
		stopNetworkJob = KtxAsync.launch {
			val client = client
			KtxAsync.launch { client?.stop() }
			this@Net.client = null
			server?.stop()
			server = null
		}
	}
}
