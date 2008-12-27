//--------------------------------------------------------------------------------
// Ferrari3D
// Game
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;

import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.core3d.game.Display;
import com.dennisbijlsma.core3d.renderer.jme.JMEOverlay;
import com.dennisbijlsma.ferrari3d.graphics.SAT;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Laptime;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.data.Resources;
import com.dennisbijlsma.util.data.TextFormatter;
import com.dennisbijlsma.util.swing.Utils2D;



public class HUD extends JMEOverlay {
	
	private Display viewport;
	private Session session;	
	private Contestant target;
	private String message;	
	private int messageTime;
	private Map<String,Object> gamedata;
	
	private Image speedo;
	private Image gears;
	private Image needle1;
	private Image needle2;
	private Image startlights;
	private Image chequeredFlag;
	private Image warningFlag;
	private AffineTransform tempTransform;
	
	private SAT sat;
	private int satLastInt;
	private int satBestLap;

	private static final String GAME_DATA_FRAMERATE="framerate";
	private static final String GAME_DATA_UPS="ups";
	private static final String GAME_DATA_POLYGONS="polygons";
	private static final String GAME_DATA_START_TIMER="startTimer";
	private static final String GAME_DATA_FINISH_TIMER="finishTimer";
	private static final int HUD_WIDTH=1024;
	private static final int HUD_HEIGHT=128;
	private static final float REFRESH_TIME=0.1f;
	private static final Font DATA_FONT=new Font("Verdana",Font.PLAIN,12);
	private static final Font POS_FONT=new Font("Verdana",Font.BOLD,24);
	private static final Font DEBUG_FONT=new Font("Verdana",Font.BOLD,10);
	private static final Color FONT_COLOR=new Color(255,255,255);
	private static final Color SHADOW_COLOR=new Color(0,0,0,128);
	private static final Color PANEL_COLOR=new Color(0,0,0,128);
	private static final Color MESSAGE_BACKGROUND=new Color(0,0,0,128);
	private static final Color MESSAGE_FOREGROUND=new Color(255,255,255);
	private static final int MESSAGE_TIME=30;
	private static final int SAT_TIME=30;
	private static final float SAT_DISTANCE=250f;
	private static final Color RADAR_BACKGROUND=new Color(0,0,0,128);
	private static final Color RADAR_TARGET=new Color(255,255,0);
	private static final Color RADAR_OTHERS=new Color(255,255,255);
	private static final float RADAR_SCALE=0.5f;
	
	
	
	public HUD(Display viewport,Session session) {
		
		super(viewport,0,viewport.getHeight()-HUD_HEIGHT,getWidth(viewport),HUD_HEIGHT);

		this.viewport=viewport;
		this.session=session;
		
		setUpdateTime(REFRESH_TIME);
		start();
		
		// Set game data
		
		gamedata=new HashMap<String,Object>();
		gamedata.put(GAME_DATA_FRAMERATE,0f);
		gamedata.put(GAME_DATA_POLYGONS,0);
		gamedata.put(GAME_DATA_START_TIMER,0f);
		gamedata.put(GAME_DATA_FINISH_TIMER,0f);
		
		// Load images
		
		speedo=Utils.loadImage("data/graphics/speedo.png");
		gears=Utils.loadImage("data/graphics/gears.png");		
		needle1=Utils.loadImage("data/graphics/needle1.png");
		needle2=Utils.loadImage("data/graphics/needle2.png");
		startlights=Utils.loadImage("data/graphics/startlights.png");
		chequeredFlag=Utils.loadImage("data/graphics/chequeredflag.png");
		warningFlag=Utils.loadImage("data/graphics/warningflag.png");
		tempTransform=new AffineTransform();
		
		// SAT
		
		sat=new SAT();
		satLastInt=0;
		satBestLap=Laptime.TIME_NOT_SET;
	}
	
	
	
	@Override
	public void paintHUD(Graphics2D g2) {
				
		if (target==null) {
			return;
		}
		
		if (Settings.getInstance().showSAT) {
			paintSAT(g2);
		}
		
		if (Settings.getInstance().showRadar) {
			paintRadar(g2);
		} else {
			paintSpeedo(g2);
		}
		
		if (Settings.getInstance().debug) {
			paintDebug(g2);
		}
				
		paintInfo(g2);
		paintMessage(g2);
	}
	
	
	
	private void paintInfo(Graphics2D g2) {
		
		Settings settings=Settings.getInstance();
		int right=viewport.getWidth();
		
		// Panels
		
		g2.setColor(PANEL_COLOR);
		g2.fillRoundRect(right-160,getHeight()-65,150,25,10,10);
		g2.fillRoundRect(right-160,getHeight()-35,150,25,10,10);
		
		// Texts
		
		String posText=getPosFormat(session.getRacePosition(target));
		Laptime currentLap=target.getCurrentLaptime();
		int currentTime=currentLap.getCurrentTime();
		
		if (sat.getScreen()==SAT.Screen.TIME_DIFFERENCE) {
			if (target.getIntermediate()==Laptime.SECTOR_1) {
				currentLap=target.getLaptime(target.getLap()-1);
				currentTime=currentLap.getLaptime();
			} else {
				currentTime=currentLap.getIntermediateTime(target.getIntermediate()-1);
			}
		}
		
		g2.setFont(DATA_FONT);
		g2.setColor(FONT_COLOR);
		g2.drawString(settings.getText("game.lap"),right-150,getHeight()-45);
		g2.drawString(target.getLap()+" / "+settings.laps,right-90,getHeight()-45);
		g2.drawString(settings.getText("game.time"),right-150,getHeight()-15);
		g2.drawString(Utils.timeFormat(currentTime),right-90,getHeight()-15);
		
		g2.setFont(POS_FONT);
		g2.setColor(SHADOW_COLOR);
		Utils2D.drawAlignedString(g2,posText,right-18,getHeight()-73,'r');
		g2.setColor(FONT_COLOR);
		Utils2D.drawAlignedString(g2,posText,right-20,getHeight()-75,'r');
				
		if (Settings.getInstance().showFramerate) {
			float framerate=(Float) gamedata.get(GAME_DATA_FRAMERATE);
			String fps=settings.getText("game.framerate")+" "+Math.round(framerate);
			g2.setFont(DEBUG_FONT);
			g2.setColor(SHADOW_COLOR);			
			Utils2D.drawAlignedString(g2,fps,right-9,16,'r');
			g2.setColor(FONT_COLOR);			
			Utils2D.drawAlignedString(g2,fps,right-10,15,'r');
		}
		
		// Starting lights
		
		float startTimer=(Float) gamedata.get(GAME_DATA_START_TIMER);
		
		if (startTimer>0f) {
			int x=viewport.getWidth()/2-150/2;
			int y=0;
			
			if (startTimer<1f) {
				g2.drawImage(startlights,x,y,x+150,y+80,0,0,150,80,null);				
			} else {
				g2.drawImage(startlights,x,y,x+150,y+80,0,80,150,160,null);
			}
		}
		
		// Chequered flag
		
		float finishTimer=(Float) gamedata.get(GAME_DATA_FINISH_TIMER);
		
		if (finishTimer>0f) {
			g2.drawImage(chequeredFlag,viewport.getWidth()/2-100/2,0,100,120,null);
		}
		
		// Warning flag
		
		if (target instanceof Player) {
			if (((Player) target).isPenalty()) {
				g2.drawImage(warningFlag,viewport.getWidth()/2-100/2,0,100,120,null);
			}
		}
	}
	
	
	
	private void paintSpeedo(Graphics2D g2) {
		
		// Panels
		
		g2.setColor(PANEL_COLOR);
		g2.fillRoundRect(75,95,110,25,10,10);
		
		// Speedo image(s)
		
		int x=10;
		int y=0;
		
		g2.drawImage(speedo,x,y,null);		
		g2.drawImage(gears,x+77,y+75,x+77+21,y+75+31,(target.getGear()+2)*25,0,
				(target.getGear()+2)*25+21,31,null);
		
		tempTransform.setToTranslation(x,y);
		tempTransform.rotate(0.000202*target.getRPM()+Math.PI,60,59);
		g2.drawImage(needle2,tempTransform,null);
		
		tempTransform.setToTranslation(x,y);
		tempTransform.rotate(0.0131*target.getSpeed(true)+Math.PI,60,59);
		g2.drawImage(needle1,tempTransform,null);
		
		// Speed text
				
		String suffix=Settings.getInstance().getText("game.kmh");
		String speedText=Math.abs(Math.round(target.getSpeed(true)))+" "+suffix;
		
		if (Settings.getInstance().units==Settings.UNITS_MPH) {
			suffix=Settings.getInstance().getText("game.mph");
			speedText=Math.abs(Math.round(1.6f*target.getSpeed(true)))+" "+suffix;
		}
		
		if (Settings.getInstance().units==Settings.UNITS_MS) {
			suffix=Settings.getInstance().getText("game.ms");
			speedText=Math.abs(Math.round(target.getSpeed(false)))+" "+suffix;
		}
		
		g2.setFont(DATA_FONT);
		g2.setColor(FONT_COLOR);
		g2.drawString(speedText,x+110,getHeight()-15);
	}
	
	
	
	private void paintRadar(Graphics2D g2) {
		
		int x=10;
		int y=4;
		int size=120;
					
		g2.setColor(RADAR_BACKGROUND);
		g2.fillOval(x,y,size,size);
				
		for (Contestant i : session.getContestantsSet()) {			
			Vector3D d1=new Vector3D(target.getPosition());
			Vector3D d2=new Vector3D(i.getPosition());
			Vector3D distance=new Vector3D();
			distance.relativeTo(d1,d2,target.getOrientation(),'y');
			
			int radarX=(x+size/2)+Math.round(distance.z*RADAR_SCALE);
			int radarY=(y+size/2)+Math.round(distance.x*RADAR_SCALE);
			
			if ((radarX>x) && (radarY>y) && (radarX<x+size) && (radarY<y+size)) {
				g2.setColor(RADAR_OTHERS);	
				g2.fillOval(radarX-5,radarY-5,10,10);
			}
		}
		
		g2.setColor(RADAR_TARGET);
		g2.fillOval(x+size/2-5,y+size/2-5,10,10);
	}
	
	
	
	private void paintSAT(Graphics2D g2) {
		
		sat.setSurface(g2,0,0,viewport.getWidth(),HUD_HEIGHT);
		sat.tick();
		
		// Events
		
		if (satLastInt!=target.getIntermediate()) {
			sat.setScreen(SAT.Screen.TIME_DIFFERENCE,SAT_TIME);
			satLastInt=target.getIntermediate();
		}
		
		if ((sat.getScreen()==SAT.Screen.TIME_DIFFERENCE) && (sat.getTimer()==1)) {
			if (Math.random()>0.66f) {
				sat.setScreen(SAT.Screen.STANDINGS,SAT_TIME);
			}
		}
		
		Contestant fastest=session.getFastestLap();
		if (fastest.getBestLaptime().getLaptime()<satBestLap) {
			sat.setScreen(SAT.Screen.FASTEST_LAP,SAT_TIME);
			satBestLap=fastest.getBestLaptime().getLaptime();
		}
		
		// Paint SAT
		
		Vector3D position=target.getPosition();
		CircuitPoint line=target.getCircuit().getIntermediate(target.getIntermediate());		
		boolean near=(Utils.getDistance(position.x,position.z,line.pointX,line.pointY)<SAT_DISTANCE);		
		int pos=session.getRacePosition(target);
		Contestant leader=session.getRacePositionID(1);
		Contestant inFront=session.getRacePositionID(pos-1);
		Contestant inBehind=session.getRacePositionID(pos+1);
		
		// Paint fastest lap screen
		
		if (sat.getScreen()==SAT.Screen.FASTEST_LAP) {
			sat.paintFastestLapScreen(fastest.getID(),fastest.getCar().getCarName(),fastest.getBestLaptime());
			return;
		}
		
		// Paint standings screen
		
		if (sat.getScreen()==SAT.Screen.STANDINGS) {
			String[] bestNames=new String[Math.min(session.getNumContestants(),8)];
			Laptime[] bestTimes=new Laptime[Math.min(session.getNumContestants(),8)];
			for (int i=0; i<bestNames.length; i++) {
				bestNames[i]=session.getRacePositionID(i+1).getID();
				bestTimes[i]=session.getRacePositionID(i+1).getBestLaptime();
			}
			sat.paintStandingsScreen(bestNames,bestTimes);
			return;
		}
		
		// Paint time diff screen (1)
		
		if ((session.getMode()==Session.SessionMode.TIME) && (near) && (target.getLap()>1)) {
			sat.paintTimeDiffScreen(target.getID(),target.getCurrentLaptime(),leader.getID(),
					leader.getBestLaptime(),target.getIntermediate(),false,session.getRacePosition(target));
			return;
		}
		
		// Paint time diff screen (2)
		
		if ((session.getMode()==Session.SessionMode.TIME) && (sat.getScreen()==SAT.Screen.TIME_DIFFERENCE)) {
			if (satBestLap!=Laptime.TIME_NOT_SET) {
				int targetInt=getLastIntermediate(satLastInt);
				Laptime targetLap=targetInt==2 ? target.getPreviousLaptime() : target.getCurrentLaptime();
				sat.paintTimeDiffScreen(target.getID(),targetLap,leader.getID(),leader.getBestLaptime(),
						targetInt,true,session.getRacePosition(target));
				return;
			}
		}
		
		// Paint pos diff screen
		
		if ((session.getMode()==Session.SessionMode.RACE) && (sat.getScreen()==SAT.Screen.TIME_DIFFERENCE)) {
			if (inFront!=null) {
				sat.paintPosDiffScreen(inFront.getID(),pos-1,target.getID(),pos,0);
				return;
			}
		}
		
		// Paint default screen
		
		if ((session.getMode()==Session.SessionMode.TIME) && (near) && (target.getLap()<2)) { 
			sat.paintDefaultScreen(target.getID(),target.getCar().getCarName());
			return;
		}
	}
	
	
	
	private void paintMessage(Graphics2D g2) {
		
		if (message==null) {
			return;
		}

		if (messageTime==0) {
			message=null;
			return;
		}
		
		messageTime--;
		
		int messageWidth=g2.getFontMetrics(DATA_FONT).stringWidth(message);
		int messageHeight=g2.getFontMetrics(DATA_FONT).getHeight();
				
		g2.setColor(MESSAGE_BACKGROUND);
		g2.fillRoundRect(viewport.getWidth()/2-messageWidth/2-10,0,messageWidth+20,messageHeight+10,10,10);
		g2.setFont(DATA_FONT);
		g2.setColor(MESSAGE_FOREGROUND);
		Utils2D.drawAlignedString(g2,message,viewport.getWidth()/2,messageHeight,'c');
	}
	
	
	
	private void paintDebug(Graphics2D g2) {
		
		Settings settings=Settings.getInstance();		
		Runtime runtime=Runtime.getRuntime();
		int processors=runtime.availableProcessors();
		String arch=System.getProperty("os.arch");
		long memory=Utils.getConsumedMemory();
		float framerate=(Float) gamedata.get(GAME_DATA_FRAMERATE);
		float ups=(Float) gamedata.get(GAME_DATA_UPS);
		
		g2.setColor(MESSAGE_BACKGROUND);
		g2.fillRect(10,0,200,85);
		g2.setColor(MESSAGE_FOREGROUND);
		g2.drawRect(10,0,200,85);
		g2.setFont(DEBUG_FONT);		
		g2.drawString(settings.getText("game.framerate")+" "+Math.round(framerate),20,15);
		g2.drawString(settings.getText("game.ups")+" "+Math.round(ups),20,30);
		g2.drawString(settings.getText("game.processors")+" "+processors+" ("+arch+")",20,45);
		g2.drawString(settings.getText("game.memory")+" "+TextFormatter.memoryFormat(memory),20,60);
		g2.drawString("Java "+Resources.getJavaVersion()+" / "+Resources.getPlatform(),20,75);
	}
	
	
	
	public void cleanup() {
	
		stop();
	}
		
	
	
	public void setTarget(Contestant newTarget) {
	
		if ((target!=newTarget) && (newTarget!=null)) {
			target=newTarget;
			setMessage(newTarget.getID());
		}
	}
	
	
	
	public Contestant getTarget() {
	
		return target;
	}
	
	
	
	public void setMessage(String message) {
		
		this.message=message;
		messageTime=MESSAGE_TIME;
	}
	
	
	
	public void setGameData(float framerate,float ups,long polygons,float startTimer,float finishTimer) {
	
		gamedata.put(GAME_DATA_FRAMERATE,framerate);
		gamedata.put(GAME_DATA_UPS,ups);
		gamedata.put(GAME_DATA_POLYGONS,polygons);
		gamedata.put(GAME_DATA_START_TIMER,startTimer);
		gamedata.put(GAME_DATA_FINISH_TIMER,finishTimer);
	}
	
	
	
	private int getLastIntermediate(int intermediate) {
		
		if (intermediate==0) { return 2; }
		if (intermediate==1) { return 0; }
		if (intermediate==2) { return 1; }
		throw new IllegalArgumentException("Invalid intermediate");
	}
	
	
	
	private String getPosFormat(int p) {
	
		if (p==1) { return Settings.getInstance().getText("game.p1"); }
		if (p==2) { return Settings.getInstance().getText("game.p2"); } 
		if (p==3) { return Settings.getInstance().getText("game.p3"); }
		return p+Settings.getInstance().getText("game.pn");
	}
	
	
	
	private static int getWidth(Display viewport) {
		
		if (viewport.getWidth()<=HUD_WIDTH) {
			return HUD_WIDTH;
		} else {
			return HUD_WIDTH*2;
		}
	}
}