package misterbander.crazyeights.scene2d.dialogs

import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.crazyeights.HUE_SLIDER_STYLE
import misterbander.crazyeights.MainMenu
import misterbander.crazyeights.TEXT_BUTTON_STYLE

class ColorPickerDialog(mainMenu: MainMenu, parent: PlayDialog) : CrazyEightsDialog(mainMenu, "Choose Your Color")
{
	private val colorCircle = scene2d.image("colorcircle")
	private val hueSlider = scene2d.slider(min = 0F, max = 360F, step = 1F, style = HUE_SLIDER_STYLE) {
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
			add(scene2d.textButton("Apply", TEXT_BUTTON_STYLE) {
				onChange {
					mainMenu.click.play()
					hide()
					game.user.color.fromHsv(hueSlider.value, 0.8F, 0.8F)
					parent.colorButton.image.color.fromHsv(hueSlider.value, 0.8F, 0.8F)
				}
			}).prefWidth(224F)
			add(scene2d.textButton("Cancel", TEXT_BUTTON_STYLE) {
				onChange { mainMenu.click.play(); hide() }
			})
		}
	}
	
	override fun show()
	{
		super.show()
		hueSlider.value = game.user.color.toHsv(FloatArray(3))[0]
	}
}
