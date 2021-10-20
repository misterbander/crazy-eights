package misterbander.crazyeights.scene2d.dialogs

import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.crazyeights.CrazyEightsScreen
import misterbander.crazyeights.INFO_LABEL_STYLE_S
import misterbander.crazyeights.TEXT_BUTTON_STYLE
import misterbander.gframework.util.wrap

class MessageDialog(screen: CrazyEightsScreen) : CrazyEightsDialog(screen, "")
{
	private val messageLabel = scene2d.label("", INFO_LABEL_STYLE_S)
	private val textButton = scene2d.textButton("", TEXT_BUTTON_STYLE) {
		labelCell.prefWidth(192F)
		onChange {
			screen.click.play()
			actionlessHide()
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
	
	fun show(title: String, message: String, buttonText: String, hideAction: (() -> Unit)? = null) =
		show(title, message, buttonText, hideAction, hideAction)
	
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
	
	fun actionlessHide() = super.hide()
}
