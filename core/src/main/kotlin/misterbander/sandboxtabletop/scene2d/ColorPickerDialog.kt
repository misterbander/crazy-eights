package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import ktx.actors.onChange
import ktx.scene2d.*
import ktx.style.*
import misterbander.sandboxtabletop.HUE_SLIDER_STYLE
import misterbander.sandboxtabletop.MenuScreen
import misterbander.sandboxtabletop.TEXT_BUTTON_STYLE

class ColorPickerDialog(screen: MenuScreen, parent: RoomDialog) : SandboxTabletopDialog(screen, "Choose Your Color")
{
	private val colorCircle = Image(game.skin.get<Drawable>("colorcircle"))
	private val hueSlider = scene2d.slider(min = 0F, max = 360F, step = 1F, style = HUE_SLIDER_STYLE, skin = game.skin) {
		onChange { colorCircle.color.fromHsv(value, 0.8F, 0.8F) }
	}
	
	init
	{
		contentTable.apply {
			add(hueSlider).padLeft(16F).padRight(16F).prefWidth(600F)
			row()
			add(colorCircle)
		}
		buttonTable.apply {
			add(scene2d.textButton("Apply", TEXT_BUTTON_STYLE, game.skin) {
				onChange {
					screen.click.play()
					hide()
					game.userColor.fromHsv(hueSlider.value, 0.8F, 0.8F)
					parent.colorButton.image.color.fromHsv(hueSlider.value, 0.8F, 0.8F)
				}
			}).prefWidth(224F)
			add(scene2d.textButton("Cancel", TEXT_BUTTON_STYLE, game.skin) {
				onChange { screen.click.play(); hide() }
			})
		}
	}
	
	override fun show()
	{
		super.show()
		hueSlider.value = game.userColor.toHsv(FloatArray(3))[0]
	}
}
