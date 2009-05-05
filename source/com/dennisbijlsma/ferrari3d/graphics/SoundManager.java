//--------------------------------------------------------------------------------
// Ferrari3D
// SoundManager
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.dennisbijlsma.core3d.scene.Sound;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.util.ResourceFile;
import com.dennisbijlsma.util.Platform;

/**
 * Stores all sounds used by the game and controls playback of them. By using this
 * class the playback of all sounds is kept in one place.
 */

public class SoundManager {
	
	private Map<SoundKey,Sound> sounds;
	
	private static SoundManager instance;
	
	public enum SoundKey {
		CAR_HIGH,
		CAR_LOW,
		CAR_NEUTRAL,
		GEAR,
		HIT,
		AMBIENT,
		CROWD
	}

	/**
	 * Creates a new <code>SoundManager</code> that will immediately start 
	 * preloading all sounds used by the game.
	 */
	
	private SoundManager() {
		
		sounds = new HashMap<SoundKey,Sound>();
		
		try {
			loadSounds();
		} catch (IOException e) {
			Settings.getInstance().getLogger().error("Could not load sounds", e);
		}
	}
	
	/**
	 * Loads all sounds that are used by the game into memory. This method must
	 * be called before any sounds can be played.
	 * @throws IOException when one of the sounds could not be loaded.
	 */
	
	protected void loadSounds() throws IOException {
		
		sounds.put(SoundKey.CAR_HIGH, loadSound("data/sounds/car_high.ogg"));
		sounds.put(SoundKey.CAR_LOW, loadSound("data/sounds/car_low.ogg"));
		sounds.put(SoundKey.CAR_NEUTRAL, loadSound("data/sounds/car_neutral.ogg"));
		sounds.put(SoundKey.GEAR, loadSound("data/sounds/gear.ogg"));
		sounds.put(SoundKey.HIT, loadSound("data/sounds/hit.ogg"));
		sounds.put(SoundKey.AMBIENT, loadSound("data/sounds/ambient.ogg"));
		sounds.put(SoundKey.CROWD, loadSound("data/sounds/crowd.ogg"));
	}
	
	/**
	 * Loads a sound from the specified location and returns it as a <code>Sound
	 * </code> object.
	 * @throws IOException when the sound could not be loaded.
	 * @throws NullPointerException when the sound could not be located.
	 */
	
	protected Sound loadSound(String path) throws IOException {
		ResourceFile resource = Platform.getResourceFile(path);
		return new Sound(resource.getURL());
	}
	
	/**
	 * Returns the sound with the specified key. When a sound with that key does
	 * not exist this method returns <code>null</code>.
	 */
	
	public Sound getSound(SoundKey key) {
		return sounds.get(key);
	}
	
	/**
	 * Plays the sound with the specified key. The sound will play once and will
	 * then stop again. This is a shorthand version of <code>getSound(SoundKey).
	 * play()</code>.
	 */
	
	public void playSound(SoundKey key) {
		getSound(key).play();
	}
	
	/**
	 * Returns the only existing instance of this class.
	 */
	
	public static SoundManager getInstance() {
	
		if (instance == null) {
			instance = new SoundManager();
		}
		
		return instance;
	}
}