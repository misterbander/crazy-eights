package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Table
import ktx.actors.onChange
import ktx.app.Platform
import ktx.scene2d.*
import ktx.style.*
import misterbander.crazyeights.LABEL_SMALL_STYLE
import misterbander.crazyeights.Room

class HelpPanel(room: Room) : Table()
{
	init
	{
		add(scene2d.table {
			defaults().space(32F)
			image(Scene2DSkin.defaultSkin.get<TextureRegion>(if (Platform.isMobile) "help_m_1" else "help_1"))
			image(Scene2DSkin.defaultSkin.get<TextureRegion>(if (Platform.isMobile) "help_m_2" else "help_2"))
			image(Scene2DSkin.defaultSkin.get<TextureRegion>(if (Platform.isMobile) "help_m_3" else "help_3"))
			image(Scene2DSkin.defaultSkin.get<TextureRegion>(if (Platform.isMobile) "help_m_4" else "help_4"))
			row()
			label(
				if (Platform.isMobile) "Tap and drag to move cards" else "Left click and drag to move cards",
				LABEL_SMALL_STYLE
			) { wrap = true }.cell(preferredWidth = 240F).inCell.top()
			label(
				if (Platform.isMobile) "Pinch with two fingers to rotate cards" else "Mouse wheel while dragging to rotate cards",
				LABEL_SMALL_STYLE
			) { wrap = true }.cell(preferredWidth = 240F).inCell.top()
			label("Stack cards", LABEL_SMALL_STYLE) { wrap = true }.cell(preferredWidth = 240F).inCell.top()
			label(
				if (Platform.isMobile) "Long press then drag to move card groups" else "Hold shift then drag to move card groups",
				LABEL_SMALL_STYLE
			) { wrap = true }.cell(preferredWidth = 240F).inCell.top()
		}).expand().bottom()
		row()
		add(scene2d.textButton("Game Rules") {
			onChange {
				room.click.play()
				Gdx.net.openURI("https://github.com/misterbander/crazy-eights/wiki/Rules")
			}
		})
		row()
		add(scene2d.label(
			"This is your hand. Cards placed here\nwill only be visible to you.", LABEL_SMALL_STYLE)
		).expandX().prefHeight(143F)
		background = Scene2DSkin.defaultSkin["chat_text_field_background"]
		isVisible = false
	}
}
