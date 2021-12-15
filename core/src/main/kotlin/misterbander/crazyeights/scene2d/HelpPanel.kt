package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Table
import ktx.actors.onChange
import ktx.app.Platform
import ktx.scene2d.*
import ktx.style.*
import misterbander.crazyeights.INFO_LABEL_STYLE_S
import misterbander.crazyeights.Room
import misterbander.crazyeights.TEXT_BUTTON_STYLE

class HelpPanel(room: Room) : Table()
{
	init
	{
		add(scene2d.table {
			defaults().space(32F)
			image(Scene2DSkin.defaultSkin.get<TextureRegion>(if (Platform.isMobile) "helpm1" else "help1"))
			image(Scene2DSkin.defaultSkin.get<TextureRegion>(if (Platform.isMobile) "helpm2" else "help2"))
			image(Scene2DSkin.defaultSkin.get<TextureRegion>(if (Platform.isMobile) "helpm3" else "help3"))
			image(Scene2DSkin.defaultSkin.get<TextureRegion>(if (Platform.isMobile) "helpm4" else "help4"))
			row()
			label(
				if (Platform.isMobile) "Tap and drag to move cards" else "Left click and drag to move cards",
				INFO_LABEL_STYLE_S
			) { wrap = true }.cell(preferredWidth = 240F).inCell.top()
			label(
				if (Platform.isMobile) "Pinch with two fingers to rotate cards" else "Mouse wheel while dragging to rotate cards",
				INFO_LABEL_STYLE_S
			) { wrap = true }.cell(preferredWidth = 240F).inCell.top()
			label("Stack cards", INFO_LABEL_STYLE_S) { wrap = true }.cell(preferredWidth = 240F).inCell.top()
			label(
				if (Platform.isMobile) "Long press then drag to move card groups" else "Hold shift then drag to move card groups",
				INFO_LABEL_STYLE_S
			) { wrap = true }.cell(preferredWidth = 240F).inCell.top()
		}).expand().bottom()
		row()
		add(scene2d.textButton("Game Rules", TEXT_BUTTON_STYLE) {
			onChange {
				room.click.play()
				Gdx.net.openURI("https://github.com/misterbander/crazy-eights/wiki/Rules")
			}
		})
		row()
		add(scene2d.label(
			"This is your hand. Cards placed here\nwill only be visible to you.", INFO_LABEL_STYLE_S)
		).expandX().prefHeight(143F)
		background = Scene2DSkin.defaultSkin["chatbackground"]
		isVisible = false
	}
}
