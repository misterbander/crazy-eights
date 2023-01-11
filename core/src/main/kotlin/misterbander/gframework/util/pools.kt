package misterbander.gframework.util

import com.badlogic.gdx.utils.Pools

/**
 * @return An object obtained from the global pool using [Pools.obtain].
 */
inline fun <reified T> obtain(): T = Pools.obtain(T::class.java)
