/*
 * This file is part of Siebe Projects samples.
 *
 * Siebe Projects samples is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Siebe Projects samples is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with Siebe Projects samples.  If not, see <http://www.gnu.org/licenses/>.
 */
package misterbander.crazyeights

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import com.badlogic.gdx.Gdx
import misterbander.gframework.scene2d.KeyboardHeightObserver

/**
 * The keyboard height provider, this class uses a `PopupWindow`
 * to calculate the window height when the floating keyboard is opened and closed.
 */
class KeyboardHeightProvider @SuppressLint("InflateParams") constructor(
	/** The root activity that uses this `KeyboardHeightProvider`. */
	private val activity: Activity
) : PopupWindow(activity)
{
	/** The keyboard height observer. */
	private var observer: KeyboardHeightObserver? = null
	
	/** The view that is used to calculate the keyboard height. */
	private val popupView: View
	
	/** The parent view. */
	private val parentView: View
	
	private var prevKeyboardHeight = 0
	
	/** Construct a new `KeyboardHeightProvider`. */
	init
	{
		val inflator = activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		popupView = inflator.inflate(R.layout.popup_window, null, false)
		contentView = popupView
		softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
		inputMethodMode = INPUT_METHOD_NEEDED
		parentView = activity.findViewById(android.R.id.content)
		width = 0
		height = WindowManager.LayoutParams.MATCH_PARENT
		popupView.viewTreeObserver.addOnGlobalLayoutListener { handleOnGlobalLayout() }
	}
	
	/**
	 * Start the `KeyboardHeightProvider`, this must be called after the `onResume()` of the `Activity`.
	 * `PopupWindow`s are not allowed to be registered before the onResume has finished
	 * of the `Activity`.
	 */
	fun start()
	{
		if (!isShowing && parentView.windowToken != null)
		{
			setBackgroundDrawable(ColorDrawable(0))
			showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0)
		}
	}
	
	/**
	 * Close the keyboard height provider,
	 * this provider will not be used anymore.
	 */
	fun close()
	{
		observer = null
		dismiss()
	}
	
	/**
	 * Set the keyboard height observer to this provider. The
	 * observer will be notified when the keyboard height has changed.
	 * For example when the keyboard is opened or closed.
	 * @param observer the observer to be added to this provider
	 */
	fun setKeyboardHeightObserver(observer: KeyboardHeightObserver?)
	{
		this.observer = observer
	}
	
	/**
	 * Popup window itself is as big as the window of the `Activity`.
	 * The keyboard can then be calculated by extracting the popup view bottom
	 * from the activity window height.
	 */
	private fun handleOnGlobalLayout()
	{
		val metrics = DisplayMetrics()
		activity.windowManager.defaultDisplay.getMetrics(metrics)
		val rect = Rect()
		popupView.getWindowVisibleDisplayFrame(rect)
		
		// REMIND, you may like to change this using the fullscreen size of the phone
		// and also using the status bar and navigation bar heights of the phone to calculate
		// the keyboard height. But this worked fine on a Nexus.
		val orientation = screenOrientation
		val keyboardHeight = metrics.heightPixels - rect.bottom
		when
		{
			keyboardHeight == 0 -> notifyKeyboardHeightChanged(0, orientation)
			orientation == Configuration.ORIENTATION_PORTRAIT -> notifyKeyboardHeightChanged(keyboardHeight, orientation)
			else -> notifyKeyboardHeightChanged(keyboardHeight, orientation)
		}
	}
	
	private val screenOrientation: Int
		get() = activity.resources.configuration.orientation
	
	private fun notifyKeyboardHeightChanged(height: Int, orientation: Int)
	{
		if (height == prevKeyboardHeight || height > Gdx.graphics.height)
			return
		prevKeyboardHeight = height
		observer?.onKeyboardHeightChanged(height, orientation)
	}
}
