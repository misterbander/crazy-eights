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
	
	suspend fun createAndStartServer(port: Int): CrazyEightsServer
	{
		stopNetworkJob?.join()
		info("Network | INFO") { "Starting server on port $port..." }
		// Create the server in a separate thread to avoid nasty lag spike
		server = CrazyEightsServer()
		withContext(asyncContext) { server!!.start(port) }
		return server!!
	}
	
	suspend fun createAndConnectClient(ip: String, port: Int): CrazyEightsClient
	{
		stopNetworkJob?.join()
		client = CrazyEightsClient()
		withContext(asyncContext) { client!!.connect(ip, port) }
		return client!!
	}
	
	fun stop()
	{
		stopNetworkJob = KtxAsync.launch {
			val stopServerJob = server?.stopAsync()
			val stopClientJob = client?.stopAsync()
			stopServerJob?.await()
			stopClientJob?.await()
			info("Network | INFO") { "Network stopped" }
			server = null
			client = null
		}
	}
}
