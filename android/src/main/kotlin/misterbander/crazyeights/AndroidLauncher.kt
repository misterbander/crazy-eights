package misterbander.crazyeights

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import misterbander.gframework.GScreen
import misterbander.gframework.scene2d.KeyboardHeightObserver

/** Launches the Android application.  */
class AndroidLauncher : AndroidApplication(), KeyboardHeightObserver
{
	private lateinit var crazyEights: CrazyEights
	private lateinit var keyboardHeightProvider: KeyboardHeightProvider
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		crazyEights = CrazyEights()
		initialize(crazyEights, AndroidApplicationConfiguration())
		
		keyboardHeightProvider = KeyboardHeightProvider(this)
		
		// Make sure to start the keyboard height provider after the onResume
		// of this activity. This is because a popup window must be initialised
		// and attached to the activity root view
		val rootView = window.decorView.rootView
		rootView.post { keyboardHeightProvider.start() }
	}
	
	override fun onPause()
	{
		super.onPause()
		keyboardHeightProvider.setKeyboardHeightObserver(null)
	}
	
	override fun onResume()
	{
		super.onResume()
		keyboardHeightProvider.setKeyboardHeightObserver(this)
	}
	
	override fun onDestroy()
	{
		super.onDestroy()
		keyboardHeightProvider.close()
	}
	
	override fun onKeyboardHeightChanged(height: Int, orientation: Int)
	{
		val gScreen = crazyEights.shownScreen as? GScreen<*> ?: return
		gScreen.keyboardHeightObservers.forEach {
			it.onKeyboardHeightChanged(height, orientation)
		}
	}
}
