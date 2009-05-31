//--------------------------------------------------------------------------------
// Ferrari3D
// SAT
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.dennisbijlsma.ferrari3d.util.LapTime;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.TextFormatter;
import com.dennisbijlsma.util.swing.Utils2D;

/**
 * Utility class for showing the speed-and-time panels. These panels support
 * different skins, the default skin resembles the Formula TV graphics from around
 * 1998 to 2002.<br><br>
 * This class paints its graphics using Java 2D, to any surface that has a <code>
 * Graphics2D</code> object. In most cases it will be desired to paint to a part
 * of the surface, for this purpose a painting area can be specified.
 */

public class SAT {

	private Graphics2D g2;
	private Rectangle area;
	
	private Screen screen;
	private int timer;
		
	private static final Font FONT=Utils.loadFont("data/fonts/manana.ttf",Font.PLAIN,15f);
	private static final Font BIG_FONT=Utils.loadFont("data/fonts/manana.ttf",Font.PLAIN,40f);
	private static final Color FONT_COLOR_MAIN=new Color(255,255,255);
	private static final Color FONT_COLOR_SUB=new Color(0,200,255);
	private static final Color FONT_COLOR_TIME=new Color(255,200,0);
	private static final Color FONT_COLOR_INFO=new Color(0,200,255);
	private static final Color FONT_COLOR_DIFF=new Color(255,220,0);
	private static final Color FONT_COLOR_POS=new Color(0,0,0);
	private static final Color FONT_COLOR_POS_1=new Color(0,0,0);
	private static final Color BACKGROUND_COLOR=new Color(0,0,0,128);
	private static final Color MAIN_PANEL_COLOR=null;
	private static final Color SUB_PANEL_COLOR=null;
	private static final Color CENTER_PANEL_COLOR=null;
	private static final Color DIFF_COLOR_PLUS=null;
	private static final Color DIFF_COLOR_MIN=null;
	private static final Color POS_COLOR=new Color(255,220,0);
	private static final Color POS_COLOR_1=new Color(255,220,0);
	private static final int MAIN_PANEL_WIDTH=150;
	private static final int SUB_PANEL_WIDTH=120;
	private static final int CENTER_PANEL_WIDTH=100;
	private static final int STANDINGS_NAME_WIDTH=120;
	private static final int STANDINGS_TIME_WIDTH=60;
	private static final int PANEL_HEIGHT=25;
	private static final int SPACING=10;
	private static final int PADDING=5;
	
	public static enum Screen {
		TIME_DIFFERENCE,
		FASTEST_LAP,
		STANDINGS
	}
	
	/**
	 * Creates a new SAT object. The surface must be set before any screens can
	 * be painted.
	 */
	
	public SAT() {

		g2=null;
		area=new Rectangle(0,0,0,0);
	}
	
	/**
	 * Sets the graphics surface where the screens will be painted. This method
	 * must be called before any painting is performed.
	 */
	
	public void setSurface(Graphics2D g2,int x,int y,int w,int h) {
	
		this.g2=g2;
		this.area.setBounds(x,y,w,h);
	}
	
	/**
	 * Marks the specified screen as currently selected. The screen will stay
	 * selected for the specified time.
	 */
	
	public void setScreen(Screen screen,int timer) {
		
		this.screen=screen;
		this.timer=timer;
	}
	
	/**
	 * Returns the currently selected screen. This method will return <code>null
	 * </code> after the timer has expired.
	 */
	
	public Screen getScreen() {
		
		return screen;
	}
	
	/**
	 * Performs a 'tick' for the current frame. SAT screens will only stay active
	 * for a certain number of ticks.
	 */
	
	public void tick() {
		
		timer--;
		if (timer<=0) {
			timer=0;
			screen=null;
		}
	}
	
	/**
	 * Returns the current value of the timer. When this timer hits zero the 
	 * current screen will be removed.
	 */
	
	public int getTimer() {
		
		return timer;
	}
	
	
	
	public void paintDefaultScreen(String driver,String car,int pos) {
				
		driver=nameFormat(driver);
		car=car.toUpperCase();
		
		paintBackground();
		paintPanel(driver,FONT,MAIN_PANEL_COLOR,FONT_COLOR_MAIN,'l',1,MAIN_PANEL_WIDTH,1,'l');
		paintPanel(car,FONT,SUB_PANEL_COLOR,FONT_COLOR_SUB,'l',2,MAIN_PANEL_WIDTH,1,'l');
		
		if (pos>0) {
			paintPanel(""+pos,BIG_FONT,POS_COLOR,FONT_COLOR_POS,'r',1,PANEL_HEIGHT*2,2,'c');
		}
	}
	
	
	
	public void paintDefaultScreen(String driver,String car) {
	
		paintDefaultScreen(driver,car,0);
	}
	
	
	
	public void paintTimeScreen(String driver,LapTime laptime) {
		
		driver=nameFormat(driver);
		String time=Utils.timeFormat(laptime.getTime());
		time=time.substring(0,time.length()-2);

		paintBackground();
		paintPanel(driver,FONT,MAIN_PANEL_COLOR,FONT_COLOR_MAIN,'l',1,MAIN_PANEL_WIDTH,1,'l');
		paintPanel(time,FONT,SUB_PANEL_COLOR,FONT_COLOR_TIME,'l',2,SUB_PANEL_WIDTH,1,'r');
	}
	
	
	
	public void paintTimeDiffScreen(String driver1,LapTime laptime1,String driver2,
			LapTime laptime2,int intermediate,boolean diff,int pos) {
		
		driver1=nameFormat(driver1);
		driver2=nameFormat(driver2);
		
		String time1=Utils.timeFormat(laptime1.getIntermediateTime(intermediate));
		String time2=Utils.timeFormat(laptime2.getIntermediateTime(intermediate));
		String timeDiff=Utils.timeDiffFormat(laptime1.getIntermediateTime(intermediate),laptime2.getIntermediateTime(intermediate));
		String intText=getIntermediateLabel(intermediate);
		
		if (!diff) {
			time1=time1.substring(0,time1.length()-2);
		}
		
		paintBackground();
		paintPanel(driver1,FONT,MAIN_PANEL_COLOR,FONT_COLOR_MAIN,'l',1,MAIN_PANEL_WIDTH,1,'l');
		paintPanel(time1,FONT,SUB_PANEL_COLOR,FONT_COLOR_TIME,'l',2,SUB_PANEL_WIDTH,1,'r');
		paintPanel(intText,FONT,CENTER_PANEL_COLOR,FONT_COLOR_INFO,'c',4,CENTER_PANEL_WIDTH,1,'c');

		if (intermediate!=2) {
			paintPanel(driver2,FONT,MAIN_PANEL_COLOR,FONT_COLOR_MAIN,'r',1,MAIN_PANEL_WIDTH,1,'r');
			paintPanel(time2,FONT,SUB_PANEL_COLOR,FONT_COLOR_TIME,'r',2,SUB_PANEL_WIDTH,1,'l');
		} else {
			paintPosRect(pos,area.width/2+CENTER_PANEL_WIDTH/2,SPACING,true);
		}			
		
		if (diff) {
			paintPanel(timeDiff,FONT,DIFF_COLOR_PLUS,FONT_COLOR_DIFF,'c',3,CENTER_PANEL_WIDTH,1,'c');
		}
	}
	
	
	
	public void paintPosDiffScreen(String driver1,int pos1,String driver2,int pos2,int diff) {
			
		driver1=nameFormat(driver1);
		driver2=nameFormat(driver2);
		String timeDiff=Utils.timeDiffFormat(diff,0);
		int posX1=area.width/2-CENTER_PANEL_WIDTH/2-MAIN_PANEL_WIDTH-PANEL_HEIGHT*2;
		int posX2=area.width/2+CENTER_PANEL_WIDTH/2+MAIN_PANEL_WIDTH;
		
		paintBackground();
		paintPanel(driver1,FONT,MAIN_PANEL_COLOR,FONT_COLOR_MAIN,'l',1,MAIN_PANEL_WIDTH,1,'l');
		paintPanel(driver2,FONT,MAIN_PANEL_COLOR,FONT_COLOR_MAIN,'r',1,MAIN_PANEL_WIDTH,1,'r');
		paintPosRect(pos1,posX1,SPACING,true);
		paintPosRect(pos2,posX2,SPACING,true);

		if (diff>0) {
			paintPanel(timeDiff,FONT,DIFF_COLOR_PLUS,FONT_COLOR_DIFF,'c',1,CENTER_PANEL_WIDTH,1,'c');
		}
	}
	
	/**
	 * Paints a notification screen that a new fastest lap has been set. The screen
	 * will show the name of the driver and the laptime.
	 */
	
	public void paintFastestLapScreen(String driver,String car,LapTime laptime) {
		
		driver=nameFormat(driver);
		String time=Utils.timeFormat(laptime);
		String info=Settings.getInstance().getText("game.fastestlap").toUpperCase();
		
		paintBackground();
		paintPanel(driver,FONT,MAIN_PANEL_COLOR,FONT_COLOR_MAIN,'l',1,MAIN_PANEL_WIDTH,1,'l');
		paintPanel(car.toUpperCase(),FONT,SUB_PANEL_COLOR,FONT_COLOR_SUB,'l',2,MAIN_PANEL_WIDTH,1,'l');
		paintPanel(info,FONT,MAIN_PANEL_COLOR,FONT_COLOR_INFO,'r',1,MAIN_PANEL_WIDTH,1,'r');
		paintPanel(time,FONT,SUB_PANEL_COLOR,FONT_COLOR_TIME,'r',2,MAIN_PANEL_WIDTH,1,'r');
	}
	
	
	
	public void paintStandingsScreen(String[] drivers,LapTime[] laptimes) {
	
		paintBackground();
		
		for (int i=0; i<Math.min(drivers.length,8); i++) {
			String driver=nameFormat(drivers[i]);
			String time=Utils.timeDiffFormat(laptimes[i],laptimes[0]);
			if (i==0) { time=Utils.timeFormat(laptimes[i]); }
			
			int x1=area.width/2-STANDINGS_NAME_WIDTH-STANDINGS_TIME_WIDTH-PANEL_HEIGHT;
			int x2=x1+PANEL_HEIGHT;
			int x3=x2+STANDINGS_NAME_WIDTH;
			int y=SPACING+i*PANEL_HEIGHT;
			
			if (i>3) {
				x1+=STANDINGS_NAME_WIDTH+STANDINGS_TIME_WIDTH+PANEL_HEIGHT;
				x2+=STANDINGS_NAME_WIDTH+STANDINGS_TIME_WIDTH+PANEL_HEIGHT;
				x3+=STANDINGS_NAME_WIDTH+STANDINGS_TIME_WIDTH+PANEL_HEIGHT;
				y-=PANEL_HEIGHT*4;
			}
			
			paintPosRect(i+1,x1,y,false);
			paintTextRect(driver,FONT,MAIN_PANEL_COLOR,FONT_COLOR_MAIN,x2,y,STANDINGS_NAME_WIDTH,PANEL_HEIGHT,'l',PADDING,PADDING);
			paintTextRect(time,FONT,SUB_PANEL_COLOR,FONT_COLOR_TIME,x3,y,STANDINGS_TIME_WIDTH,PANEL_HEIGHT,'r',PADDING,PADDING);
		}
	}
	
	/**
	 * Paints the background panel. If the area to paint is the same size as the 
	 * last time, the background is painted from an image.
	 */
	
	private void paintBackground() {
				
		if (BACKGROUND_COLOR==null) {
			return;
		}
		
		g2.setColor(BACKGROUND_COLOR);
		g2.fillRect(area.x,area.y,area.width,area.height);
	}
	
	
	
	private void paintRect(Color color,int x,int y,int width,int height) {
		
		if (color!=null) {
			g2.setColor(color);
			g2.fillRect(x,y,width,height);
		}
	}
	
	
	
	private void paintText(String text,Font font,Color color,int x,int y,char align) {
	
		g2.setFont(font);
		g2.setColor(color);
		Utils2D.drawAlignedString(g2,text,x,y,align);
	}
	
	
	
	private void paintTextRect(String text,Font font,Color bgColor,Color textColor,int x,int y,
			int width,int height,char align,int padX,int padY) {
		
		paintRect(bgColor,x,y,width,height);
		
		if (align=='l') { paintText(text,font,textColor,x+padX,y+height-padY,'l'); }
		if (align=='c') { paintText(text,font,textColor,x+width/2,y+height-padY,'c'); }
		if (align=='r') { paintText(text,font,textColor,x+width-padX,y+height-padY,'r'); }
	}
	
	
	
	private void paintPanel(String text,Font font,Color bgColor,Color textColor,char column,
			int row,int width,int lines,char align) {
		
		int x=area.x+area.width/2;
		int y=SPACING+(row-1)*PANEL_HEIGHT;
		
		if (column=='l') { x-=CENTER_PANEL_WIDTH/2+width; }
		if (column=='c') { x-=CENTER_PANEL_WIDTH/2; }
		if (column=='r') { x+=CENTER_PANEL_WIDTH/2; }
		
		paintTextRect(text,font,bgColor,textColor,x,y,width,PANEL_HEIGHT*lines,align,PADDING,PADDING*lines);
	}
	
	
	
	private void paintPosRect(int pos,int x,int y,boolean large) {
		
		Font font=(!large) ? FONT : BIG_FONT;
		int size=(!large) ? PANEL_HEIGHT : PANEL_HEIGHT*2;
		Color bgColor=(pos==1) ? POS_COLOR_1 : POS_COLOR;
		Color textColor=(pos==1) ? FONT_COLOR_POS : FONT_COLOR_POS_1;
		
		paintTextRect(""+pos,font,bgColor,textColor,x+1,y+1,size-2,size-2,'c',PADDING,large ? PADDING*2 : PADDING);
	}
	
	/**
	 * Returns the textual description of the intermediate with the specified
	 * index.
	 */
	
	private String getIntermediateLabel(int intermediate) {
		
		Settings settings=Settings.getInstance();
	
		switch (intermediate) {
			case 0 : return settings.getText("game.intermediate1").toUpperCase();
			case 1 : return settings.getText("game.intermediate2").toUpperCase();
			case 2 : return settings.getText("game.intermediate3").toUpperCase();
			default : return "???";
		}
	}
	
	/**
	 * Converts the specified string to a name. Depending on the current SAT skin
	 * the name may be in uppercase and without initials.
	 */
	
	private String nameFormat(String name) {
	
		return TextFormatter.nameFormat(name.toUpperCase(),false);
	}
}