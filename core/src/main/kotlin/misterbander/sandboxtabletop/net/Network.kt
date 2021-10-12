package misterbander.sandboxtabletop.net

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.log.info

class Network
{
	private val asyncContext = newSingleThreadAsyncContext("Network-AsyncExecutor-Thread")
	var server: SandboxTabletopServer? = null
		private set
	var client: SandboxTabletopClient? = null
		private set
	private var stopNetworkJob: Job? = null
	
	suspend fun createAndStartServer(port: Int): SandboxTabletopServer
	{
		stopNetworkJob?.join()
		info("Network | INFO") { "Starting server on port $port..." }
		withContext(asyncContext) {
			server = SandboxTabletopServer().apply { start(port) }
		}
		return server!!
	}
	
	suspend fun createAndConnectClient(ip: String, port: Int): SandboxTabletopClient
	{
		stopNetworkJob?.join()
		withContext(asyncContext) {
			client = SandboxTabletopClient().apply { connect(ip, port) }
		}
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
