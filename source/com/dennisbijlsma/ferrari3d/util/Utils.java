//--------------------------------------------------------------------------------
// Ferrari3D
// Utils
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.util.DataLoader;
import com.dennisbijlsma.util.ResourceFile;
import com.dennisbijlsma.util.Resources;
import com.dennisbijlsma.util.TextFormatter;

/**
 * Miscellenous utility and convenience methods. This class can also be used for 
 * loading of simple resource files such as images.
 */

public class Utils {
	
	/**
	 * Private constructor as this class should not be initialized.
	 */
	
	private Utils() {
			
	}
	
	/**
	 * Returns true if the application is currently running in Webstart mode, and
	 * false when it is currently running in local mode.
	 */
	
	public static boolean isWebstart() {
	
		return Resources.isWebstart(new File("data"));
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
	
	public static String timeFormat(Laptime t) {
	
		return timeFormat(t.getLaptime());
	}
	
	/**
	 * Convience version of <code>TextFormatter.timeDiffFormat(int,int)</code>.
	 */
	
	public static String timeDiffFormat(Laptime t1,Laptime t2) {
	
		return timeDiffFormat(t1.getLaptime(),t2.getLaptime());
	}
	
	/**
	 * Convience version of <code>TextFormatter.timeDiffFormat(int,int)</code>.
	 */
	
	public static String timeDiffFormat(int t1,int t2) {
	
		return TextFormatter.timeDiffFormat(t1,t2);
	}
	
	/**
	 * Returns the distance between two points.
	 * @param x1 The x-coordinate for the first point.
	 * @param y1 The y-coordinate for the first point.
	 * @param x2 The x-coordinate for the second point.
	 * @param y2 The y-coordinate for the second point.
	 * @return The distance between the two points.
	 */
	
	public static float getDistance(float x1,float y1,float x2,float y2) {
		
		float xDistance=Math.abs(x1-x2);
		float yDistance=Math.abs(y1-y2);
		
		return (float) Math.sqrt(xDistance*xDistance+yDistance*yDistance);
	}
	
	/**
	 * Returns the distance between two points. The (x,z) plane is used to
	 * convert the vectors to a two-dimensional form.
	 * @param p1 The first point.
	 * @param p2 The second point.
	 * @return The distance between the two points.
	 */
	
	public static float getDistance(Vector3D p1,Vector3D p2) {
	
		return getDistance(p1.x,p1.y,p2.x,p2.y);
	}
	
	/**
	 * Returns the color for the specified hex string. For example, using the
	 * string <code>#FF0000</code> as a parameter would return the color red.
	 */
	
	public static Color getColor(String hex) {
		
		return Color.decode(hex);
	}
	
	/**
	 * Returns the specified color as hexadecimal string. For example, entering 
	 * the color red would return the value <code>#FF0000</code>.
	 */
	
	public static String getColor(Color color) {
	
		return "#"+Integer.toHexString(color.getRGB()).substring(2,8).toUpperCase();
	}
		
	/**
	 * Returns a value in milliseconds. This value should only be used for
	 * relative timing, not for absolute.
	 */
	
	public static long getTimestamp() {
	
		return (System.nanoTime()/1000/1000);
	}
	
	/**
	 * Returns the amount of memory consumed by the JVM, in bytes. This amount is
	 * independant from the currently set heap size.
	 */
	
	public static long getConsumedMemory() {
	
		Runtime runtime=Runtime.getRuntime();
		return runtime.totalMemory()-runtime.freeMemory();
	}
	
	/**
	 * Loads an image from the specified resource file.
	 * @throws NullPointerException when the image could not be located.
	 */
	
	public static BufferedImage loadImage(ResourceFile resource) {
	
		try {
			InputStream stream=resource.getStream();
			BufferedImage image=DataLoader.loadImage(stream);
			stream.close();
			return image;
		} catch (IOException e) {
			throw new NullPointerException("Could not locate image: "+resource.getPath());
		}
	}
	
	/**
	 * Covenience method for loading images. This method redirects to <code>
	 * DataLoader.loadImage(URL)</code>.
	 */
	
	//TODO will be deprecated
	public static Image loadImage(String path) {
	
		try {
			return DataLoader.loadImage(Resources.getResourceFile(path).getStream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Covenienve method for loading fonts. This method redirects to <code>
	 * DataLoader.loadFont(URL,int,float)</code>.
	 */
	
	public static Font loadFont(String path,int style,float size) {
	
		try {
			return DataLoader.loadFont(Resources.getResourceFile(path).getStream(),style,size);
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
	 * Returns the display mode object for the specified string. The string should 
	 * be in the format width x height. When a supported display mode could not be
	 * found, the default is returned.
	 */
	
	public static Dimension getDisplayMode(String s) {
		
		int width=Integer.parseInt(s.split("x")[0]);
		int height=Integer.parseInt(s.split("x")[1]);
		
		return new Dimension(width,height);
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
			Settings.getInstance().getLogger().warning("Could not start browser",e);
		}
	}
}