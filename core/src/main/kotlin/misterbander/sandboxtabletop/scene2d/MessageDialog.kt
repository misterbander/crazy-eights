package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import ktx.actors.onChange
import misterbander.gframework.util.wrap
import misterbander.sandboxtabletop.INFO_LABEL_STYLE
import misterbander.sandboxtabletop.SandboxTabletopScreen
import misterbander.sandboxtabletop.TEXT_BUTTON_STYLE

/**
 * Represents pop up message dialogs. Contains a message and a button. Used to display error messages and status messages.
 */
class MessageDialog(screen: SandboxTabletopScreen) : SandboxTabletopDialog(screen, "")
{
	private val messageLabel = Label("", game.skin, INFO_LABEL_STYLE)
	private val textButton: TextButton = TextButton("", game.skin, TEXT_BUTTON_STYLE).apply {
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
		contentTable.add(messageLabel)
		buttonTable.add(textButton)
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
