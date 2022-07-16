package misterbander.crazyeights.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import misterbander.crazyeights.CrazyEights

/** Launches the desktop (LWJGL3) application. */
fun main(args: Array<String>)
{
	Lwjgl3Application(CrazyEights(args), Lwjgl3ApplicationConfiguration().apply {
		setTitle("Crazy Eights")
		setWindowedMode(1280, 720)
		setWindowIcon("icon128.png", "icon64.png", "icon32.png", "icon16.png")
	})
}
