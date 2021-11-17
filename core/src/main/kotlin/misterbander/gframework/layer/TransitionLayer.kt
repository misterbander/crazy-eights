package misterbander.gframework.layer

import com.badlogic.gdx.scenes.scene2d.EventListener
import misterbander.gframework.GFramework
import misterbander.gframework.GScreen

/**
 * [GLayer] handling the rendering of transition effects and provides event callbacks or automatic switching of screens
 * when the transition ends.
 *
 * This default implementation draws nothing. You can implement your own transitions by extending this class and
 * overriding [render] to draw the transitions.
 *
 * All events to the parent's stage and uiStage will be cancelled when an out transition is playing.
 * @param screen the parent [GScreen]
 */
open class TransitionLayer<T : GFramework>(protected val screen: GScreen<T>) : GLayer
{
	/** Whether the transition is currently playing. Will be set to true when [start] is called. */
	var isRunning = false
		protected set
	/**
	 * True if the transition is being played right after switching to this screen. False if the transition is being
	 * played right before switching to another screen.
	 */
	protected var isTransitioningIn = false
	/** Elapsed time in seconds after the transition begins. Used for calculating [progress]. */
	protected var time = 0F
	/** How long the transition lasts in seconds. */
	protected open val duration: Float
		get() = 0F
	/** Progress of the transition from 0 to 1. Calculated by dividing the elapsed time by the duration. */
	val progress: Float
		get() = if (duration == 0F) 0F else time/duration
	private var onTransitionEnd: () -> Unit = {}
	
	private val eventCanceller = EventListener { event ->
		event.cancel()
		false
	}
	
	/**
	 * Begins the transition while providing a callback function when the transition ends.
	 * @param progress progress to start the transition, ranges from 0 to 1
	 * @param isTransitioningIn true if the transition is started after switching to this screen
	 * @param onTransitionEnd will be called when the transition ends
	 */
	fun start(progress: Float = 0F, isTransitioningIn: Boolean = false, onTransitionEnd: () -> Unit = {})
	{
		if (!isTransitioningIn)
		{
			screen.stage.addCaptureListener(eventCanceller)
			screen.uiStage.addCaptureListener(eventCanceller)
		}
		time = progress*duration
		isRunning = true
		this.isTransitioningIn = isTransitioningIn
		this.onTransitionEnd = onTransitionEnd
	}
	
	/**
	 * Begins the transition and switch to another screen when the transition ends.
	 * @param progress progress to start the transition, ranges from 0 to 1
	 * @param targetScreen the screen the game will switch to when the transition ends
	 * @param startTargetScreenTransition if true, then the 'in' transition will be played right after switching to the
	 * target screen
	 */
	fun start(progress: Float = 0F, targetScreen: GScreen<*>, startTargetScreenTransition: Boolean = true)
	{
		start(progress) {
			screen.game.setScreen(targetScreen::class.java)
			if (startTargetScreenTransition)
				targetScreen.transition.start(isTransitioningIn = true)
		}
	}
	
	override fun update(delta: Float)
	{
		if (isRunning)
		{
			time += delta
			if (time > duration)
			{
				screen.stage.removeCaptureListener(eventCanceller)
				screen.uiStage.removeCaptureListener(eventCanceller)
				time = duration
				isRunning = false
				onTransitionEnd()
			}
		}
	}
	
	/**
	 * Renders the transition. Draw your own transition effects here. You can use [progress] which indicates the current
	 * phase of the transition.
	 */
	override fun render(delta: Float) = Unit
}
