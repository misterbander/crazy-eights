package misterbander.sandboxtabletop

import com.badlogic.gdx.audio.Sound
import misterbander.gframework.GScreen

abstract class SandboxTabletopScreen(game: SandboxTabletop) : GScreen<SandboxTabletop>(game)
{
	val click: Sound = game.assetManager["sounds/click.wav"]
}
