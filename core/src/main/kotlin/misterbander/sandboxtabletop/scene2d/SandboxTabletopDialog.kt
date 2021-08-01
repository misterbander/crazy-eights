package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.utils.Align
import ktx.actors.alpha
import ktx.actors.centerPosition
import ktx.actors.onChange
import misterbander.gframework.scene2d.AccessibleInputDialog
import misterbander.sandboxtabletop.ANIMATION_DURATION
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.SandboxTabletopScreen

/**
 * Represents generic windows. All windows will inherit from this class.
 */
abstract class SandboxTabletopDialog(
	protected val screen: SandboxTabletopScreen,
	title: String,
) : AccessibleInputDialog(title, screen.game.skin, "windowstyle")
{
	protected val game: SandboxTabletop = screen.game
	val closeButton: Button = Button(game.skin, "closebuttonstyle").apply {
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
			Actions.parallel(
				Actions.fadeIn(ANIMATION_DURATION, Interpolation.exp5Out),
				Actions.scaleTo(1F, 1F, ANIMATION_DURATION, Interpolation.exp5Out)
			)
		)
		centerPosition()
	}
	
	override fun hide()
	{
		hide(
			Actions.parallel(
				Actions.fadeOut(ANIMATION_DURATION, Interpolation.exp5In),
				Actions.scaleTo(0.95F, 0.95F, ANIMATION_DURATION, Interpolation.exp5In)
			)
		)
	}
}
