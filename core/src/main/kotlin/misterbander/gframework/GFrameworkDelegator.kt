package misterbander.gframework

import ktx.app.KtxApplicationAdapter

/**
 * Delegates all [KtxApplicationAdapter] method calls to a [GFramework] instance, that only gets initialized in [create].
 *
 * This is to avoid having to clutter properties in [GFramework] with `lateinit` modifiers or needing to use `lazy`
 * delegates, and allows direct property instantiation using `val`s.
 */
class GFrameworkDelegator(private val initializer: () -> GFramework) : KtxApplicationAdapter
{
	private lateinit var gFramework: GFramework
	
	override fun create()
	{
		gFramework = initializer()
		gFramework.create()
	}
	
	override fun resize(width: Int, height: Int) = gFramework.resize(width, height)
	
	override fun render() = gFramework.render()
	
	override fun pause() = gFramework.pause()
	
	override fun resume() = gFramework.resume()
	
	override fun dispose() = gFramework.dispose()
}
