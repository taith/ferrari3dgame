//-----------------------------------------------------------------------------
// Ferrari3D
// Utils
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.dennisbijlsma.core3d.Sound;
import com.dennisbijlsma.core3d.scene.BufferedImageTexture;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.ResourceFile;
import nl.colorize.util.FormatUtils;

/**
 * Miscellenous utility and convenience methods. This class can also be used for 
 * loading of simple resource files such as images.
 */
public final class Utils {
	
	/**
	 * Private constructor, utility class.
	 */
	private Utils() { 
		
	}
	
	/**
	 * Returns true if the application is currently running in Webstart mode, and
	 * false when it is currently running in local mode.
	 */
	public static boolean isWebstart() {
		return !new File("data").exists();
	}
	
	public static String timeFormat(int t, boolean simple) {
		String formatted = FormatUtils.timeFormat(t);
		if (simple) {
			formatted = formatted.substring(0, formatted.length() - 2);
		}
		return formatted;
	}
	
	public static String timeFormat(Laptime t) {
		return timeFormat(t.getTime(), false);
	}
	
	public static String timeDiffFormat(int t1, int t2) {
		if ((t1 == Laptime.TIME_NOT_SET) || (t2 == Laptime.TIME_NOT_SET)) {
			return "";
		}
		return FormatUtils.timeDiffFormat(t1, t2);
	}
	
	public static String timeDiffFormat(Laptime t1, Laptime t2) {
		return timeDiffFormat(t1.getTime(), t2.getTime());
	}
	
	/**
	 * Returns the amount of memory consumed by the JVM, in bytes. This amount is
	 * independant from the currently set heap size.
	 */
	public static long getConsumedMemory() {
		Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}
	
	/**
	 * Loads an image from the specified resource file.
	 * @throws NullPointerException when the image could not be located.
	 */
	public static BufferedImage loadImage(ResourceFile resource) {
		try {
			return LoadUtils.loadImage(resource.getStream());
		} catch (IOException e) {
			throw new NullPointerException("Could not locate image: " + resource.getPath());
		}
	}
	
	/**
	 * Loads an image from the resource file at the specified path.
	 * @throws NullPointerException when the image could not be located.
	 */
	public static BufferedImage loadImage(String path) {
		return loadImage(new ResourceFile(path));
	}
	
	/**
	 * Loads an image from the specified path as a texture.
	 * @throws NullPointerException when the image could not be located.
	 * @throws InvalidTextureException if the image is not a valid texture.
	 */
	public static BufferedImageTexture loadTexture(String path) {
		return new BufferedImageTexture(loadImage(path));
	}
	
	/**
	 * Covenience method for loading fonts. This method redirects to <code>
	 * DataLoader.loadFont(URL,int,float)</code>.
	 */
	public static Font loadFont(String path, int style, float size) {
		try {
			return LoadUtils.loadFont(new ResourceFile(path).getStream(), style, size);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Loads a sound from the resource file at the specified path. The sound will
	 * not be playing initially.
	 */
	public static Sound loadSound(String path) {
		Sound sound = new Sound(new ResourceFile(path).getURL());
		sound.stop();
		return sound;
	}
	
	/**
	 * Returns the default display mode. This is the mode that is currently active 
	 * at the time this method is called.
	 */
	public static Dimension getCurrentDisplayMode() {
		return Toolkit.getDefaultToolkit().getScreenSize();
	}
	
	/**
	 * Starts the default browser and navigates it to the specified URL. When the 
	 * browser could not be started this method does nothing.
	 */
	public static void openBrowser(String url) {
		try {
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().browse(new URI(url));
			}
		} catch (Exception e) {
			Settings.getInstance().getLogger().warning("Could not start browser", e);
		}
	}
}
