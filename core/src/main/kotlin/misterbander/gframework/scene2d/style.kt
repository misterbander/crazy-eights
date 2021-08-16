package misterbander.gframework.scene2d

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import ktx.style.*

/**
 * @param name name of the style as it will appear in the [Skin] instance.
 * @param extend optional name of an _existing_ style of the same type. Its values will be copied and used as base for
 * this style.
 * @param init will be applied to the style instance. Inlined.
 * @return A new instance of [GTextField.GTextFieldStyle] added to the [Skin] with the selected name.
 */
@SkinDsl
inline fun Skin.gTextField(
	name: String,
	extend: String? = null,
	init: (@SkinDsl GTextField.GTextFieldStyle).() -> Unit = {}
): GTextField.GTextFieldStyle =
	addStyle(name, if (extend == null) GTextField.GTextFieldStyle() else GTextField.GTextFieldStyle(get(extend)), init)
