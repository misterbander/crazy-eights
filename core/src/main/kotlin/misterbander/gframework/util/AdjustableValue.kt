package misterbander.gframework.util

import com.badlogic.gdx.Gdx
import kotlin.reflect.KProperty

class AdjustableValue(
	private var value: Float,
	private val positiveKey: Int,
	private val negativeKey: Int,
	private val adjustingSpeed: Float
)
{
	operator fun getValue(from: Any?, property: KProperty<*>): Float
	{
		var changed = false
		if (Gdx.input.isKeyPressed(positiveKey))
		{
			value += Gdx.graphics.deltaTime*adjustingSpeed
			changed = true
		}
		if (Gdx.input.isKeyPressed(negativeKey))
		{
			value -= Gdx.graphics.deltaTime*adjustingSpeed
			changed = true
		}
		if (changed)
			println("${property.name} = $value")
		return value
	}
	
	operator fun setValue(from: Any?, property: KProperty<*>, value: Float)
	{
		this.value = value
	}
}
