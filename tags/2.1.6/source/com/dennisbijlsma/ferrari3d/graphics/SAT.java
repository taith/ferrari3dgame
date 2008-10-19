//--------------------------------------------------------------------------------
// Ferrari3D
// SAT
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.dennisbijlsma.ferrari3d.util.Laptime;
import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.data.TextFormatter;
import com.dennisbijlsma.util.swing.Utils2D;



public class SAT {

	private Graphics2D g2;
	private Rectangle area;
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
	
	
	
	public SAT() {

		g2=null;
		area=new Rectangle(0,0,1,1);
	}
	
	
	
	public void paintDefaultScreen(String driver,String car,int pos) {
				
		driver=nameFormat(driver.toUpperCase());
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
	
	
	
	public void paintTimeScreen(String driver,Laptime laptime) {
		
		driver=nameFormat(driver.toUpperCase());
		String time=Utils.timeFormat(laptime.getCurrentTime());
		time=time.substring(0,time.length()-2);

		paintBackground();
		paintPanel(driver,FONT,MAIN_PANEL_COLOR,FONT_COLOR_MAIN,'l',1,MAIN_PANEL_WIDTH,1,'l');
		paintPanel(time,FONT,SUB_PANEL_COLOR,FONT_COLOR_TIME,'l',2,SUB_PANEL_WIDTH,1,'r');
	}
	
	
	
	public void paintTimeDiffScreen(String driver1,Laptime laptime1,String driver2,
			Laptime laptime2,int intermediate,boolean diff,int pos) {
		
		driver1=nameFormat(driver1.toUpperCase());
		driver2=nameFormat(driver2.toUpperCase());
		
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
			
		driver1=nameFormat(driver1.toUpperCase());
		driver2=nameFormat(driver2.toUpperCase());
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
	
	
	
	public void paintStandingsScreen(String[] drivers,Laptime[] laptimes) {
	
		paintBackground();
		
		for (int i=0; i<Math.min(drivers.length,8); i++) {
			String driver=nameFormat(drivers[i].toUpperCase());
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
	
	
	
	private String getIntermediateLabel(int intermediate) {
	
		switch (intermediate) {
			case 0 : return "INTERMEDIATE 1"; 
			case 1 : return "INTERMEDIATE 2";
			case 2 : return "START / FINISH";
			default : return "???";
		}
	}
	
	
	
	public void setGraphics(Graphics2D g2) {
	
		this.g2=g2;
	}
	
	
	
	public Graphics2D getGraphics() {
	
		return g2;
	}
	
	
	
	public void setArea(int x,int y,int width,int height) {
	
		area.setBounds(x,y,width,height);
	}
	
	
	
	public Rectangle getArea() {
	
		return area;
	}
	
	
	
	public void setTimer(int timer) {
	
		this.timer=Math.max(timer,0);
	}
	
	
	
	public int getTimer() {
	
		return timer;
	}
	
	
	
	private String nameFormat(String name) {
	
		return TextFormatter.nameFormat(name,false);
	}
}