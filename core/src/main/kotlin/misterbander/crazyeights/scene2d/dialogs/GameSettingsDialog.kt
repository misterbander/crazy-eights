package misterbander.crazyeights.scene2d.dialogs

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import ktx.actors.onChange
import ktx.actors.onKeyboardFocus
import ktx.collections.*
import ktx.scene2d.*
import misterbander.crazyeights.FORM_TEXT_FIELD_STYLE
import misterbander.crazyeights.INFO_LABEL_STYLE_S
import misterbander.crazyeights.Room
import misterbander.crazyeights.SELECT_BOX_STYLE
import misterbander.crazyeights.TEXT_BUTTON_STYLE
import misterbander.crazyeights.game.Ruleset
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.net.packets.AiAddEvent
import misterbander.crazyeights.net.packets.NewGameEvent
import misterbander.crazyeights.net.packets.RulesetUpdateEvent
import misterbander.gframework.scene2d.GTextField
import misterbander.gframework.scene2d.UnfocusListener
import misterbander.gframework.scene2d.gTextField

class GameSettingsDialog(private val room: Room) : CrazyEightsDialog(room, "Game Settings")
{
	private val items = GdxArray(Rank.values())
		.filter { it != Rank.EIGHT }
		.map { if (it == Rank.NO_RANK) "Off" else it.toString() }
	
	private val maxDrawCountTextField = scene2d.gTextField(this, "3", FORM_TEXT_FIELD_STYLE) {
		textFieldFilter = GTextField.GTextFieldFilter.DigitsOnlyFilter()
		onKeyboardFocus { focused ->
			if (focused)
				return@onKeyboardFocus
			if (text.isEmpty())
				text = "0"
			game.client?.sendTCP(RulesetUpdateEvent(
				room.ruleset.copy(maxDrawCount = if (text == "0") Int.MAX_VALUE else text.toInt()), game.user.name
			))
		}
	}
	private val drawTwosSelectBox = scene2d.selectBox<String>(SELECT_BOX_STYLE) {
		items = this@GameSettingsDialog.items
		selected = Rank.TWO.toString()
		selection.setProgrammaticChangeEvents(false)
		onChange {
			game.client?.sendTCP(RulesetUpdateEvent(room.ruleset.copy(drawTwos = selected?.toRank()), game.user.name))
		}
	}
	private val skipsSelectBox = scene2d.selectBox<String>(SELECT_BOX_STYLE) {
		items = this@GameSettingsDialog.items
		selected = Rank.QUEEN.toString()
		selection.setProgrammaticChangeEvents(false)
		onChange {
			game.client?.sendTCP(RulesetUpdateEvent(room.ruleset.copy(skips = selected?.toRank()), game.user.name))
		}
	}
	private val reversesSelectBox = scene2d.selectBox<String>(SELECT_BOX_STYLE) {
		items = this@GameSettingsDialog.items
		selected = Rank.ACE.toString()
		selection.setProgrammaticChangeEvents(false)
		onChange {
			game.client?.sendTCP(RulesetUpdateEvent(room.ruleset.copy(reverses = selected?.toRank()), game.user.name))
		}
	}
	
	private val addAiButton = scene2d.textButton("Add AI", TEXT_BUTTON_STYLE) {
		onChange {
			room.click.play()
			game.client?.sendTCP(AiAddEvent)
		}
	}
	private val newGameButton = scene2d.textButton("New Game", TEXT_BUTTON_STYLE) {
		onChange {
			room.click.play()
			hide()
			game.client?.sendTCP(NewGameEvent())
		}
	}
	
	init
	{
		contentTable.apply {
			defaults().left().space(16F)
			add(scene2d.verticalGroup {
				columnAlign(Align.left)
				label("Max Draw Count", INFO_LABEL_STYLE_S)
				label("Maximum number of cards to draw before being forced to pass.\nEnter 0 for no limit.", INFO_LABEL_STYLE_S) { color = Color.LIGHT_GRAY }
			})
			add(maxDrawCountTextField).prefWidth(128F)
			row()
			add(scene2d.verticalGroup {
				columnAlign(Align.left)
				label("Draw Twos", INFO_LABEL_STYLE_S)
				label("Next player draws two cards. Can be stacked.", INFO_LABEL_STYLE_S) { color = Color.LIGHT_GRAY }
			})
			add(drawTwosSelectBox)
			row()
			add(scene2d.verticalGroup {
				columnAlign(Align.left)
				label("Skips", INFO_LABEL_STYLE_S)
				label("Skips the next player.", INFO_LABEL_STYLE_S) { color = Color.LIGHT_GRAY }
			})
			add(skipsSelectBox)
			row()
			add(scene2d.verticalGroup {
				columnAlign(Align.left)
				label("Reverses", INFO_LABEL_STYLE_S)
				label("Reverses the play direction.", INFO_LABEL_STYLE_S) { color = Color.LIGHT_GRAY }
			})
			add(reversesSelectBox)
			row()
			add(addAiButton).colspan(2).center()
		}
		buttonTable.apply {
			add(newGameButton).prefWidth(224F)
			add(scene2d.textButton("Cancel", TEXT_BUTTON_STYLE) {
				onChange { room.click.play(); hide() }
			})
		}
		addListener(UnfocusListener(this))
	}
	
	override fun act(delta: Float)
	{
		super.act(delta)
		newGameButton.isDisabled = room.isGameStarted || room.tabletop.users.size == 1
		addAiButton.isDisabled = room.isGameStarted || room.tabletop.users.size >= 6
		maxDrawCountTextField.isDisabled = room.isGameStarted
		drawTwosSelectBox.isDisabled = room.isGameStarted
		skipsSelectBox.isDisabled = room.isGameStarted
		reversesSelectBox.isDisabled = room.isGameStarted
	}
	
	fun updateRuleset(ruleset: Ruleset)
	{
		maxDrawCountTextField.text = if (ruleset.maxDrawCount == Int.MAX_VALUE) "0" else ruleset.maxDrawCount.toString()
		drawTwosSelectBox.selected = ruleset.drawTwos?.toString() ?: "Off"
		skipsSelectBox.selected = ruleset.skips?.toString() ?: "Off"
		reversesSelectBox.selected = ruleset.reverses?.toString() ?: "Off"
	}
	
	private fun String.toRank(): Rank? = when (this)
	{
		"A" -> Rank.ACE
		"J" -> Rank.JACK
		"Q" -> Rank.QUEEN
		"K" -> Rank.KING
		"2", "3", "4", "5", "6", "7", "8", "9", "10" -> Rank.values()[toInt()]
		else -> null
	}
}
