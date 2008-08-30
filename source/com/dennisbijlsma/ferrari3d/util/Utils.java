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

import com.dennisbijlsma.core3d.*;
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
	 * Formats a floating point number to the specified number of decimal points.
	 * For example, formatting the number '1.234' with 2 decimal places would 
	 * format into '1.23'.
	 * @param number The number to format.
	 * @param decimals The amount of digits behind the comma.
	 * @return The formatted number.
	 */
	
	public static String numberFormat(float number,int decimals) {
	
		String format=""+Math.round(number*Math.pow(10,decimals))/Math.pow(10,decimals);
		
		while (format.split("\\.")[1].length()<decimals) {
			format+="0";
		}
		
		return format;
	}
	
	/**
	 * Appends a number with zeroes so that the output string is always of length
	 * <code>digits</code>. If the number is already bigger then the requested 
	 * number of digits, the original number is returned.
	 * @param n The number to format.
	 * @param digits The number of digits
	 * @return The formatted number.
	 */
	
	public static String paddedNumberFormat(int n,int digits) {
	
		String format=""+n;
		
		while (format.length()<digits) {
			format="0"+format;
		}
		
		return format;
	}
	
	/**
	 * Formats a time to a string. The time is assumed to be in millisecond precision.
	 * The time is assumed to be a relative time rather then a calendar time.
	 * When the number of hours is more then 0 the output string is in the form 
	 * H:MM:SS. If the amount of hours is 0 the output string is formatted as M:SS.
	 * @param time The time, in millisecond precision.
	 * @return The formatted time.
	 */
	
	public static String timeFormat(int time) {

		int hours=0;
		int minutes=0;
		int seconds=0;
		
		while (time>=60*60*1000) { hours++; time-=60*60*1000; }
		while (time>=60*1000) { minutes++; time-=60*1000; }
		while (time>=1000) { seconds++; time-=1000; }
		
		String format="";
		if (hours>0) {
			format+=hours+":"+paddedNumberFormat(minutes,2)+":"+paddedNumberFormat(seconds,2);
		} else {
			if (minutes>0) {
				format+=minutes+":"+paddedNumberFormat(seconds,2);
			} else {
				format+=seconds;
			}
		}
		
		format+="."+paddedNumberFormat(time,3);
		
		return format;
	}
	
	/**
	 * Convenience function for formatting a <code>Laptime</code> object. The 
	 * total time for the lap is formatted using <code>timeFormat(int)</code>.
	 * @param t The time to format.
	 * @return The formatted time.
	 */
	
	public static String timeFormat(Laptime t) {
	
		return timeFormat(t.getLaptime());
	}
	
	/**
	 * Returns the formatted difference between two times. The second time is
	 * substracted from the first, after which the result is formatted with
	 * <code>timeFormat(int)</code>.
	 * @param t1 The first time.
	 * @param t2 The second time.
	 * @return The difference between the two times as a formatted string.
	 */
	
	public static String timeDiffFormat(int t1,int t2) {
		
		String sign=(t1<t2) ? "-" : "+";
		return sign+timeFormat(Math.abs(t1-t2));
	}
	
	/**
	 * Returns a formatted string with the difference between two laptimes. This
	 * is a convenience version of <code>timeDiffFormat(int,int)</code>.
	 * @param t1 The first time.
	 * @param t2 The second time.
	 * @return The difference between the two times as a formatted string.
	 */
	
	public static String timeDiffFormat(Laptime t1,Laptime t2) {
	
		return timeDiffFormat(t1.getLaptime(),t2.getLaptime());
	}
	
	/**
	 * Returns a string in title format. Title format means that the first 
	 * character of every word is in uppercase, while the rest is in lowercase.
	 */
	
	public static String titleFormat(String s) {
	
		if ((s==null) || (s.length()==0)) { return s; }
		if (s.length()==1) { return s.toUpperCase(); }
		return s.substring(0,1).toUpperCase()+s.substring(1);
	}
	
	/**
	 * Formats a name. When both a first and a last name were entered, only the
	 * last name is returned. When only one name was entered, that is returned.
	 */
	
	public static String nameFormat(String s) {
	
		String[] parts=s.split(" ");
		
		if (parts.length==1) {
			return s;
		} else {
			return parts[parts.length-1];
		}
	}
	
	/**
	 * Returns a memory or hard disk value in formatted form. Normally the format
	 * will be in kilobytes, optionally it can be set to use megabytes instead. 
	 */
	
	public static String memoryFormat(long memory,boolean useMB) {
		
		if (!useMB) {
			return numberFormat(memory/1024f/1024f,3)+" Kb";
		} else {
			return Math.round(memory/1024f/1024f/1024f)+" Mb";
		}
	}
	
	/**
	 * Draws a string aligned to the specified x point. The currently active font
	 * is used to determine the text bounds.
	 * @param g2 The Graphics2D object to draw with.
	 * @param text The text to draw.
	 * @param x The x-coordinate the text will be aligned to.
	 * @param y The y-coordinate where the baseline of the text is located.
	 * @param a The alignment. One of 'l', 'c', 'r'.
	 */
	
	public static void drawAlignedString(Graphics2D g2,String text,int x,int y,char a) {
		
		if (a=='l') { g2.drawString(text,x,y); }
		if (a=='c') { g2.drawString(text,x-g2.getFontMetrics().stringWidth(text)/2,y); }
		if (a=='r') { g2.drawString(text,x-g2.getFontMetrics().stringWidth(text),y); }
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
		
		if ((v1==null) || (v1.length()!=5) || (v2==null) || (v2.length()!=5)) {
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
	
		return MediaLoader.loadImage(Resources.getResource(location));
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
			Settings.getInstance().getLog().warning("Could not start browser",e);
		}
	}
}