package misterbander.crazyeights.scene2d.dialogs

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import ktx.actors.onChange
import ktx.actors.onKeyboardFocus
import ktx.collections.*
import ktx.scene2d.*
import misterbander.crazyeights.FORM_TEXT_FIELD_STYLE
import misterbander.crazyeights.LABEL_SMALL_STYLE
import misterbander.crazyeights.Room
import misterbander.crazyeights.game.Ruleset
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.net.packets.AiAddEvent
import misterbander.crazyeights.net.packets.NewGameEvent
import misterbander.crazyeights.net.packets.ResetDeckEvent
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
	private val drawTwosSelectBox = scene2d.selectBox<String>() {
		items = this@GameSettingsDialog.items
		selected = Rank.TWO.toString()
		selection.setProgrammaticChangeEvents(false)
		onChange {
			game.client?.sendTCP(RulesetUpdateEvent(room.ruleset.copy(drawTwos = selected?.toRank()), game.user.name))
		}
	}
	private val skipsSelectBox = scene2d.selectBox<String>() {
		items = this@GameSettingsDialog.items
		selected = Rank.QUEEN.toString()
		selection.setProgrammaticChangeEvents(false)
		onChange {
			game.client?.sendTCP(RulesetUpdateEvent(room.ruleset.copy(skips = selected?.toRank()), game.user.name))
		}
	}
	private val reversesSelectBox = scene2d.selectBox<String>() {
		items = this@GameSettingsDialog.items
		selected = Rank.ACE.toString()
		selection.setProgrammaticChangeEvents(false)
		onChange {
			game.client?.sendTCP(RulesetUpdateEvent(room.ruleset.copy(reverses = selected?.toRank()), game.user.name))
		}
	}
	
	private val addAiButton = scene2d.textButton("Add AI") {
		onChange {
			room.click.play()
			game.client?.sendTCP(AiAddEvent)
		}
	}
	private val resetDeckButton = scene2d.textButton("Reset deck") {
		onChange {
			room.click.play()
			hide()
			game.client?.sendTCP(ResetDeckEvent())
		}
	}
	private val newGameButton = scene2d.textButton("New Game") {
		onChange {
			room.click.play()
			hide()
			game.client?.sendTCP(NewGameEvent())
		}
	}
	
	init
	{
		contentTable.add(scene2d.table {
			defaults().left().space(16F)
			verticalGroup {
				columnAlign(Align.left)
				label("Max Draw Count", LABEL_SMALL_STYLE)
				label("Maximum number of cards to draw before being forced to pass.\nEnter 0 for no limit.", LABEL_SMALL_STYLE) { color = Color.LIGHT_GRAY }
			}
			actor(maxDrawCountTextField).cell(preferredWidth = 128F)
			row()
			verticalGroup {
				columnAlign(Align.left)
				label("Draw Twos", LABEL_SMALL_STYLE)
				label("Next player draws two cards. Can be stacked.", LABEL_SMALL_STYLE) { color = Color.LIGHT_GRAY }
			}
			actor(drawTwosSelectBox)
			row()
			verticalGroup {
				columnAlign(Align.left)
				label("Skips", LABEL_SMALL_STYLE)
				label("Skips the next player.", LABEL_SMALL_STYLE) { color = Color.LIGHT_GRAY }
			}
			actor(skipsSelectBox)
			row()
			verticalGroup {
				columnAlign(Align.left)
				label("Reverses", LABEL_SMALL_STYLE)
				label("Reverses the play direction.", LABEL_SMALL_STYLE) { color = Color.LIGHT_GRAY }
			}
			actor(reversesSelectBox)
			row()
			horizontalGroup {
				space(16F)
				actor(addAiButton)
				actor(resetDeckButton)
			}.cell(colspan = 2).inCell.center()
		})
		buttonTable.add(scene2d.table {
			defaults().space(16F)
			actor(newGameButton).cell(preferredWidth = 224F)
			textButton("Cancel") {
				onChange { room.click.play(); hide() }
			}
		})
		addListener(UnfocusListener(this))
	}
	
	override fun act(delta: Float)
	{
		super.act(delta)
		newGameButton.isDisabled = room.isGameStarted || room.tabletop.users.size == 1
		addAiButton.isDisabled = room.isGameStarted || room.tabletop.users.size >= 6
		resetDeckButton.isDisabled = room.isGameStarted
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
