//--------------------------------------------------------------------------------
// Ferrari3D
// Utils
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

import com.dennisbijlsma.core3d.data.*;
import com.dennisbijlsma.xmlserver.*;
import com.dennisbijlsma.util.data.*;
import com.dennisbijlsma.util.swing.*;

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
	
		return (!new File("data").exists());
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
	 * Returns the screen size (pixel dimensions) for the current default monitor.
	 */
	
	public static Dimension getScreenSize() {
	
		return Toolkit.getDefaultToolkit().getScreenSize();
	}
	
	/**
	 * Returns the current date in the form DD-MM-YYYY. Optionally the current
	 * time can also be added in the fom HH:MM.
	 */
	
	public static String getCurrentDate(boolean time) {
		
		String format="dd-MM-yyyy"+(time ? " (HH:mm)" : "");	
		return new SimpleDateFormat(format).format(new java.util.Date());
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
	 * Returns if <code>v2</code> is newer than <code>v1</code>. Both versions 
	 * strings should be in the form <code>a.b.c</code>.
	 */
	
	public static boolean isNewerVersion(String v1,String v2) {
		
		int length1=v1.split("\\.").length;
		int length2=v2.split("\\.").length;
		
		if ((v1==null) || (length1!=3) || (v2==null) || (length2!=3)) {
			throw new IllegalArgumentException("Invalid version number string");
		}
		
		int main1=Integer.parseInt(v1.split("\\.")[0]);
		int main2=Integer.parseInt(v2.split("\\.")[0]);
		int major1=Integer.parseInt(v1.split("\\.")[1]);
		int major2=Integer.parseInt(v2.split("\\.")[1]);
		int minor1=Integer.parseInt(v1.split("\\.")[2]);
		int minor2=Integer.parseInt(v2.split("\\.")[2]);
		
		if (main2>main1) { return true; }
		if ((main2==main1) && (major2>major1)) { return true; }
		if ((main2==main1) && (major2==major1) && (minor2>minor1)) { return true; }
		return false;
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
		
		return (runtime.totalMemory()-runtime.freeMemory());
	}
	
	/**
	 * Covenienve method for loading images. This method redirects to <code>
	 * DataLoader.loadImage(URL)</code>.
	 */
	
	public static Image loadImage(String location) {
	
		return DataLoader.loadImage(Resources.getResource(location));
	}
	
	/**
	 * Covenienve method for loading fonts. This method redirects to <code>
	 * DataLoader.loadFont(URL,int,float)</code>.
	 */
	
	public static Font loadFont(String location,int style,float size) {
	
		try {
			return DataLoader.loadFont(Resources.getResourceStream(location),style,size);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Returns the default display mode. This is the mode that is currently active 
	 * at the time this method is called.
	 */
	
	public static Dimension getCurrentDisplayMode() {
		
		GraphicsEnvironment environment=GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device=environment.getDefaultScreenDevice();
		DisplayMode mode=device.getDisplayMode();
		
		return new Dimension(mode.getWidth(),mode.getHeight());
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
		
		if (!Desktop.isDesktopSupported()) {
			return;
		}
		
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (Exception e) {
			
		}
	}
}