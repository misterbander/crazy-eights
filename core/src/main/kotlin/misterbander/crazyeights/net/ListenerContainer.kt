package misterbander.crazyeights.net

import com.esotericsoftware.kryonet.Listener

interface ListenerContainer<T : Listener>
{
	var listener: T?
	
	fun newListener(): T
	
	fun registerNewListener(): T
	{
		listener = newListener()
		return listener!!
	}
	
	fun unregisterListener(): T
	{
		val clientListener = listener!!
		this.listener = null
		return clientListener
	}
}
