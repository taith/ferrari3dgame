//--------------------------------------------------------------------------------
// Ferrari3D
// Utils
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.dennisbijlsma.core3d.data.ImmutableVector3D;
import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.util.LoadUtils;
import com.dennisbijlsma.util.ResourceFile;
import com.dennisbijlsma.util.Platform;
import com.dennisbijlsma.util.TextFormatter;

/**
 * Miscellenous utility and convenience methods. This class can also be used for 
 * loading of simple resource files such as images.
 */

public class Utils {
	
	/**
	 * Private constructor, utility class.
	 */
	
	private Utils() { }
	
	/**
	 * Returns true if the application is currently running in Webstart mode, and
	 * false when it is currently running in local mode.
	 */
	
	public static boolean isWebstart() {
		return Platform.isWebstart(new File("data"));
	}
	
	/**
	 * Convience version of <code>TextFormatter.timeFormat(int)</code>.
	 */
	
	public static String timeFormat(int t) {
		return TextFormatter.timeFormat(t);
	}
		
	
	/**
	 * Convience version of <code>TextFormatter.timeFormat(int)</code>.
	 */
	
	public static String timeFormat(LapTime t) {
		return timeFormat(t.getTime());
	}
	
	/**
	 * Convience version of <code>TextFormatter.timeDiffFormat(int,int)</code>.
	 */
	
	public static String timeDiffFormat(int t1, int t2) {
		return TextFormatter.timeDiffFormat(t1, t2);
	}
	
	/**
	 * Convience version of <code>TextFormatter.timeDiffFormat(int,int)</code>.
	 */
	
	public static String timeDiffFormat(LapTime t1, LapTime t2) {
		return timeDiffFormat(t1.getTime(), t2.getTime());
	}
	
	/**
	 * Returns the distance between two points.
	 */
	
	public static float getDistance(float x1, float y1, float x2, float y2) {
		float xDistance = Math.abs(x1 - x2);
		float yDistance = Math.abs(y1 - y2);
		return (float) Math.sqrt(xDistance * xDistance + yDistance * yDistance);
	}
	
	/**
	 * Returns the distance between two points. The (x,z) plane is used to
	 * convert the vectors to a two-dimensional form.
	 * @deprecated The JavaDoc for this method is different from its working!
	 */
	
	@Deprecated
	public static float getDistance(Vector3D p1, Vector3D p2) {
		return getDistance(p1.x, p1.y, p2.x, p2.y);
	}
	
	/**
	 * Returns the distance from a vector to a circuit point.
	 */
	
	public static float getDistance(ImmutableVector3D p1, CircuitPoint p2) {
		return getDistance(p1.getX(), p1.getZ(), p2.pointX, p2.pointY);
	}
		
	/**
	 * Returns a value in milliseconds. This value should only be used for
	 * relative timing, not for absolute.
	 */
	
	public static long getTimestamp() {
		return System.nanoTime() / 1000 / 1000;
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
			InputStream stream = resource.getStream();
			BufferedImage image = LoadUtils.loadImage(stream);
			stream.close();
			return image;
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
	 * Covenienve method for loading fonts. This method redirects to <code>
	 * DataLoader.loadFont(URL,int,float)</code>.
	 */
	
	public static Font loadFont(String path, int style, float size) {
	
		try {
			return LoadUtils.loadFont(Platform.getResourceFile(path).getStream(), style, size);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
	
	/**
	 * Runs the specified command and eats the exception.
	 */
	
	public static void run(String cmd) {
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			Settings.getInstance().getLogger().warning("Could not run '" + cmd + "'", e);
		}
	}
}