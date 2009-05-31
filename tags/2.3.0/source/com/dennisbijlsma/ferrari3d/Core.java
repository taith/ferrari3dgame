//--------------------------------------------------------------------------------
// Ferrari3D
// GameController
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import com.dennisbijlsma.core3d.Display;
import com.dennisbijlsma.core3d.GameCore;
import com.dennisbijlsma.core3d.InitDisplayException;
import com.dennisbijlsma.ferrari3d.graphics.Loader;
import com.dennisbijlsma.ferrari3d.util.Settings;

/**
 * Controls the different game states (menu and in-game). A typical session starts
 * with the menu, then the game, and then the results menu. This class makes sure
 * all of these are using the correct {@link Session}.
 */

public class Core extends GameCore {
	
	private Ferrari3D parent;
	private Settings settings;

	private Session session;
	private Game game;
	private Menu menu;
	
	/**
	 * Creates a new {@code Core}.
	 * @param parent The main application class.
	 */
	
	public Core(Ferrari3D parent) {
	
		super(createDisplay());
		
		this.parent = parent;
		settings = Settings.getInstance();
	}
	
	/** {@inheritDoc} */
	
	@Override
	protected void setupGameStates() {
		
		Loader.createWorld(getContext().getSceneGraph(), getContext().getDisplay());
		
		recreateSession();
		changeToMenuState();
	}
	
	/** {@inheritDoc} */
	
	@Override
	protected void endGame() { }
	
	/**
	 * Creates a new session object. If an old session existed it is replaced.
	 * The new session will use the session mode that is currently selected.s
	 * @return The newly created {@code Session}.
	 */
	
	public Session recreateSession() {
	
		switch (settings.mode) {
			case Settings.MODE_TIME : session = new Session(Session.SessionMode.TIME); break;		
			case Settings.MODE_RACE : session = new Session(Session.SessionMode.RACE); break;
			default : throw new IllegalStateException("Invalid session mode");
		}
		
		return session;
	}
	
	/**
	 * Changes the game's state to the menu. If the game was running when this
	 * method is called it will be stopped. The first time this method is called
	 * the menu system is created, calls after that will reuse that instance.
	 */
	
	public void changeToMenuState() {
		
		if (game != null) {
			getContext().getSceneGraph().clear();
			game = null;
			Loader.recreateCameras(getContext().getSceneGraph(), getContext().getDisplay(), true);
		}
		
		if (menu == null) {
			menu = new Menu(this, session);
		} else {
			menu.changeSession(session);
		}
		
		changeGameState(menu);
		
		if (session.isFinished()) {
			menu.setSelectedMenu(Menu.Screen.RESULTS);
		}
	}
	
	/**
	 * Starts the editor. Calling this method will stop the menu or game, should
	 * they be active.
	 */
	
	public void startEditor() {
		parent.startEditor(false);
	}
	
	/**
	 * Changes the game's state to in-game. If the menu is active when this 
	 * method is called it will be stopped.
	 */
	
	public void changeToGameState() {
		game = new Game(this, session);
		changeGameState(game);
	}
	
	/**
	 * Requests that the application exits.
	 */
	
	public void exit() {
		parent.exit();
	}
	
	/** {@inheritDoc} */
	
	@Override
	protected void initDisplayFailed(InitDisplayException e) {
		super.initDisplayFailed(e);
	}
	
	/**
	 * Creates the display system based on the current settings.
	 */

	private static Display createDisplay() {
		
		Settings settings = Settings.getInstance();
		int width = Settings.WINDOW_WIDTH;
		int height = Settings.WINDOW_HEIGHT;
		if (settings.fullscreen) {
			width = settings.resolution.width;
			height = settings.resolution.height;
		}
		
		Display display = new Display(width, height, settings.fullscreen);
		display.setWindowTitle(settings.getText("game.title"));
		display.setWindowIcon(Settings.ICON);
		display.setMouseCursorVisible(true);
		return display;
	}
}