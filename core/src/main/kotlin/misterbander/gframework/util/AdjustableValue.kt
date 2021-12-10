package misterbander.gframework.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import kotlin.reflect.KProperty

class AdjustableValue(
	private var value: Float,
	private val filterKey: Int = -1,
	private val adjustingSpeed: Float
)
{
	operator fun getValue(from: Any?, property: KProperty<*>): Float
	{
		if (filterKey != -1 && !Gdx.input.isKeyPressed(filterKey))
			return value
		var changed = false
		if (Gdx.input.isKeyPressed(Input.Keys.EQUALS))
		{
			value += Gdx.graphics.deltaTime*adjustingSpeed
			changed = true
		}
		if (Gdx.input.isKeyPressed(Input.Keys.MINUS))
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
