package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import ktx.actors.onChange
import misterbander.gframework.util.wrap
import misterbander.sandboxtabletop.SandboxTabletopScreen

/**
 * Represents pop up message dialogs. Contains a message and a button. Used to display error messages and status messages.
 */
class MessageDialog(
	screen: SandboxTabletopScreen
) : SandboxTabletopDialog(screen, "")
{
	private val messageLabel = Label("", game.skin, "infolabelstyle")
	private val textButton: TextButton = TextButton("", game.skin, "textbuttonstyle").apply {
		labelCell.prefWidth(192F)
		onChange {
			screen.click.play()
			hide()
			buttonAction?.invoke()
		}
	}
	private var hideAction: (() -> Unit)? = null
	private var buttonAction: (() -> Unit)? = null
	
	init
	{
		contentTable.apply {
			pad(16F)
			add(messageLabel)
		}
		buttonTable.apply {
			pad(0F, 16F, 16F, 16F)
			add(textButton)
		}
	}
	
	fun show(title: String, message: String, buttonText: String, hideAction: (() -> Unit)?)
	{
		show(title, message, buttonText, hideAction, hideAction)
	}
	
	fun show(title: String, message: String, buttonText: String, hideAction: (() -> Unit)?, buttonAction: (() -> Unit)?)
	{
		titleLabel.setText(title)
		messageLabel.setText(messageLabel.style.font.wrap(message, 800))
		textButton.setText(buttonText)
		this.hideAction = hideAction
		this.buttonAction = buttonAction
		show()
	}
	
	override fun hide()
	{
		super.hide()
		hideAction?.invoke()
	}
}
