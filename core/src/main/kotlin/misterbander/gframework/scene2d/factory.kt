package misterbander.gframework.scene2d

import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Tree.Node
import ktx.scene2d.*
import ktx.scene2d.defaultStyle
import ktx.style.*

/**
 * @param accessibleInputDialog optional [AccessibleInputDialog] to put the text field in. If specified, an adjust focus
 * listener will be added to the text field for you
 * @param text initial text displayed by the field. Defaults to empty string.
 * @param style name of the widget style. Defaults to [defaultStyle].
 * @param skin [Skin] instance that contains the widget style. Defaults to [Scene2DSkin.defaultSkin]
 * @param init will be invoked with the widget as "this". Consumes actor container (usually a [Cell] or [Node]) that
 * contains the widget. Might consume the actor itself if this group does not keep actors in dedicated containers.
 * Inlined.
 * @return A [GTextField] instance added to this group.
 */
@Scene2dDsl
inline fun <S> KWidget<S>.gTextField(
	accessibleInputDialog: AccessibleInputDialog? = null,
	text: String = "",
	style: String = defaultStyle,
	skin: Skin = Scene2DSkin.defaultSkin,
	init: (@Scene2dDsl GTextField).(S) -> Unit = {}
): GTextField
{
	val actor = actor(GTextField(text, skin[style]), init)
	accessibleInputDialog?.addFocusListener(actor)
	return actor
}
