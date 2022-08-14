package misterbander.crazyeights.scene2d.dialogs

import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.crazyeights.CrazyEightsScreen
import misterbander.crazyeights.LABEL_SMALL_STYLE
import misterbander.gframework.util.wrap

class MessageDialog(screen: CrazyEightsScreen) : RebuildableDialog(screen, "")
{
	private var message = ""
	private var buttonText = ""
	private var hideAction: () -> Unit = {}
	private var buttonAction: () -> Unit = {}
	
	override fun build()
	{
		contentTable.add(scene2d.label(game.notoSansScSmall.wrap(message, 800), LABEL_SMALL_STYLE))
		buttonTable.add(scene2d.textButton(buttonText) {
			onChange {
				screen.click.play()
				hide(false)
				buttonAction()
			}
		}).prefWidth(224F)
	}
	
	fun show(title: String, message: String, buttonText: String, hideAction: () -> Unit = {}, buttonAction: () -> Unit = {})
	{
		titleLabel.setText(title)
		this.message = message
		this.buttonText = buttonText
		this.hideAction = hideAction
		this.buttonAction = buttonAction
		show()
	}
	
	override fun hide() = hide(true)
	
	fun hide(runHideAction: Boolean)
	{
		super.hide()
		if (runHideAction)
			hideAction()
	}
}
