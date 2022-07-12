package misterbander.crazyeights.scene2d.dialogs

import ktx.scene2d.*
import misterbander.crazyeights.CrazyEightsScreen
import misterbander.crazyeights.LABEL_SMALL_STYLE
import misterbander.gframework.util.wrap

class InfoDialog(screen: CrazyEightsScreen) : CrazyEightsDialog(screen, "")
{
	private val messageLabel = scene2d.label("", LABEL_SMALL_STYLE)
	
	init
	{
		closeButton.isDisabled = true
		contentTable.pad(32F, 96F, 32F, 96F)
		contentTable.add(messageLabel)
	}
	
	fun show(title: String, message: String)
	{
		titleLabel.setText(title)
		messageLabel.setText(messageLabel.style.font.wrap(message, 800))
		show()
	}
}
