package misterbander.sandboxtabletop.scene2d

//import com.badlogic.gdx.scenes.scene2d.ui.Table
//import misterbander.sandboxtabletop.RoomScreen
//
///**
// * This is the game menu window that displays after clicking the "Pause" button in the top left button in the room. There
// * are two buttons, one to continue and another to disconnect from the server.
// */
//class GameMenuWindow(screen: RoomScreen) : SandboxTabletopWindow(screen, "Game Menu", true)
//{
//	init
//	{
//		// Set up the UI
//		defaults().pad(16f)
//		val continueButton = TextButton("Continue", game.skin, "textbuttonstyle")
//		continueButton.addListener(screen.ChangeListener { close() })
//		val disconnectButton = TextButton("Disconnect", game.skin, "textbuttonstyle")
//		disconnectButton.addListener(screen.ChangeListener {
//			assert(screen.client != null)
//			screen.client.disconnect()
//		})
//		val table = Table()
//		table.defaults().center().space(16f)
//		table.add<TextButton>(continueButton)
//		table.row()
//		table.add<TextButton>(disconnectButton)
//		add(table)
//	}
//}