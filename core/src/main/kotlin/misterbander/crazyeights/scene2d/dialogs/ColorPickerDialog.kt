package misterbander.crazyeights.scene2d.dialogs

import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.crazyeights.HUE_SLIDER_STYLE
import misterbander.crazyeights.MainMenuScreen

class ColorPickerDialog(
	private val mainMenu: MainMenuScreen,
	private val colorButton: ImageButton
) : RebuildableDialog(mainMenu, "Choose Your Color")
{
	override fun build()
	{
		val colorCircle = scene2d.image("color_circle")
		val hueSlider = scene2d.slider(min = 0F, max = 360F, step = 1F, style = HUE_SLIDER_STYLE) {
			onChange { colorCircle.color.fromHsv(value, 0.8F, 0.8F) }
			value = game.user.color.toHsv(FloatArray(3))[0]
		}
		contentTable.add(scene2d.table {
			defaults().space(8F)
			actor(hueSlider).cell(padLeft = 16F, padRight = 16F, preferredWidth = 600F)
			row()
			actor(colorCircle)
		})
		buttonTable.add(scene2d.table {
			defaults().space(16F)
			textButton("Apply") {
				onChange {
					mainMenu.click.play()
					hide()
					game.user.color.fromHsv(hueSlider.value, 0.8F, 0.8F)
					colorButton.image.color.fromHsv(hueSlider.value, 0.8F, 0.8F)
				}
			}.cell(preferredWidth = 224F)
			textButton("Cancel") {
				onChange { mainMenu.click.play(); hide() }
			}
		})
	}
}
