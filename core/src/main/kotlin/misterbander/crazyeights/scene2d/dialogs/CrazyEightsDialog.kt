package misterbander.crazyeights.scene2d.dialogs

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.utils.Align
import ktx.actors.along
import ktx.actors.alpha
import ktx.actors.centerPosition
import ktx.actors.onChange
import ktx.actors.onKeyDown
import ktx.scene2d.*
import misterbander.crazyeights.ANIMATION_DURATION
import misterbander.crazyeights.CLOSE_BUTTON_STYLE
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.CrazyEightsScreen
import misterbander.crazyeights.WINDOW_STYLE
import misterbander.gframework.scene2d.AccessibleInputDialog

/**
 * Represents generic windows. All windows will inherit from this class.
 */
abstract class CrazyEightsDialog(
	protected val screen: CrazyEightsScreen,
	title: String,
) : AccessibleInputDialog(title, Scene2DSkin.defaultSkin, WINDOW_STYLE)
{
	protected val game: CrazyEights = screen.game
	val closeButton: Button = scene2d.button(CLOSE_BUTTON_STYLE) {
		onChange { screen.click.play(); hide() }
	}
	private val scaleFactor = 0.95F
	
	init
	{
		titleTable.add(closeButton).right()
		titleTable.pad(2F, 16F, 0F, 8F)
		contentTable.pad(16F)
		buttonTable.pad(0F, 16F, 16F, 16F)
		buttonTable.defaults().space(16F)
		
		onKeyDown { keyCode ->
			if (keyCode == Input.Keys.BACK)
				hide()
		}
	}
	
	open fun show()
	{
		pack()
		setOrigin(Align.center)
		alpha = 0F
		scaleX = scaleFactor
		scaleY = scaleFactor
		show(
			screen.uiStage,
			fadeIn(ANIMATION_DURATION, Interpolation.exp5Out) along scaleTo(1F, 1F, ANIMATION_DURATION, Interpolation.exp5Out)
		)
		centerPosition()
	}
	
	override fun hide() =
		hide(fadeOut(ANIMATION_DURATION, Interpolation.exp5In) along scaleTo(0.95F, 0.95F, ANIMATION_DURATION, Interpolation.exp5In))
}
