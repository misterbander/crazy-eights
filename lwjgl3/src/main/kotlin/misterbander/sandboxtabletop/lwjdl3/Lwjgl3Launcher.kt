package misterbander.sandboxtabletop.lwjdl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import misterbander.sandboxtabletop.SandboxTabletop

/** Launches the desktop (LWJGL3) application.  */
object Lwjgl3Launcher
{
	@JvmStatic
	fun main(args: Array<String>)
	{
		createApplication()
	}
	
	private fun createApplication(): Lwjgl3Application
	{
		return Lwjgl3Application(SandboxTabletop(), defaultConfiguration)
	}
	
	private val defaultConfiguration: Lwjgl3ApplicationConfiguration
		get()
		{
			val configuration = Lwjgl3ApplicationConfiguration()
			configuration.setTitle("SandboxTabletop")
			configuration.setWindowedMode(1280, 720)
			configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png")
			return configuration
		}
}
