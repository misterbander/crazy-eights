package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.ui.Label
import misterbander.gframework.util.wrap
import misterbander.sandboxtabletop.INFO_LABEL_STYLE
import misterbander.sandboxtabletop.SandboxTabletopScreen

class InfoDialog(screen: SandboxTabletopScreen) : SandboxTabletopDialog(screen, "")
{
	private val messageLabel = Label("", game.skin, INFO_LABEL_STYLE)
	
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
