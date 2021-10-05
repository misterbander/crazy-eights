package misterbander.sandboxtabletop.scene2d.dialogs

import ktx.scene2d.*
import misterbander.gframework.util.wrap
import misterbander.sandboxtabletop.INFO_LABEL_STYLE_S
import misterbander.sandboxtabletop.SandboxTabletopScreen

class InfoDialog(screen: SandboxTabletopScreen) : SandboxTabletopDialog(screen, "")
{
	private val messageLabel = scene2d.label("", INFO_LABEL_STYLE_S)
	
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
