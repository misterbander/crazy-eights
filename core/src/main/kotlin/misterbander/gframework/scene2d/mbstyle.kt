package misterbander.gframework.scene2d

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import ktx.style.*

/**
 * @param name name of the style as it will appear in the [Skin] instance.
 * @param extend optional name of an _existing_ style of the same type. Its values will be copied and used as base for
 * this style.
 * @param init will be applied to the style instance. Inlined.
 * @return A new instance of [MBTextField.MBTextFieldStyle] added to the [Skin] with the selected name.
 */
@SkinDsl
inline fun Skin.mbTextField(
	name: String = defaultStyle,
	extend: String? = null,
	init: (@SkinDsl MBTextField.MBTextFieldStyle).() -> Unit = {}
): MBTextField.MBTextFieldStyle
{
	return addStyle(name, if (extend == null) MBTextField.MBTextFieldStyle() else MBTextField.MBTextFieldStyle(get(extend)), init)
}
