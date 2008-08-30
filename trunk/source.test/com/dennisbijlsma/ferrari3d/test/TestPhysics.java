package com.dennisbijlsma.ferrari3d.test;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;

import com.dennisbijlsma.ferrari3d.*;
import com.dennisbijlsma.ferrari3d.graphics.*;
import com.dennisbijlsma.ferrari3d.util.*;
import com.dennisbijlsma.core3d.*;
import com.dennisbijlsma.util.swing.*;



public class TestPhysics extends JPanel implements Runnable {
	
	private Session session;
	private Contestant contestant;
	private Matrix3D transform;
	private int[] controls;
	
	private float time;
	private Image carImage;
	private Image legendImage;
	
	private static final float SCALE=1.2f;
	private static final int LEGEND_SIZE=100;
	private static final int GRAPH_SIZE=400;
	private static final int GRID_SIZE=40;
	private static final String X_LABEL="Time (s)";
	private static final String Y_LABEL="Speed (kmh)";
	private static final float X_SCALE=1f;
	private static final float Y_SCALE=40f;

	
	
	public static void main(String[] args) {
		
		try {
			Settings settings=Settings.getInstance();
			settings.setVersion(Ferrari3D.VERSION);
			settings.load();
			settings.init();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	
		JFrame frame=new JFrame("Ferrari3D | TestPhysics");
		frame.setSize(500,500);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(new TestPhysics(frame));
		frame.setVisible(true);
	}
	
	
	
	public TestPhysics(JFrame frame) {
	
		super(null);
		
		setOpaque(true);
		
		session=new Session(Session.SessionMode.TIME);
		contestant=createContestant();
		contestant.setOrientation(-1.57f);
		session.addContestant(contestant);
		transform=new Matrix3D();
		controls=new int[4];
		
		carImage=createCarImage();
		legendImage=createLegendImage();
		
		// Add listeners
		
		frame.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode()==KeyEvent.VK_UP) { controls[0]=10; }
				if (e.getKeyCode()==KeyEvent.VK_DOWN) { controls[1]=10; }
				if (e.getKeyCode()==KeyEvent.VK_LEFT) { controls[2]=10; }
				if (e.getKeyCode()==KeyEvent.VK_RIGHT) { controls[3]=10; }
			}
			
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode()==KeyEvent.VK_UP) { controls[0]=0; }
				if (e.getKeyCode()==KeyEvent.VK_DOWN) { controls[1]=0; }
				if (e.getKeyCode()==KeyEvent.VK_LEFT) { controls[2]=0; }
				if (e.getKeyCode()==KeyEvent.VK_RIGHT) { controls[3]=0; }
			}
		});
		
		// Animation thread
		
		Thread t=new Thread(this);
		t.start();
	}
	
	
	
	public void run() {
	
		while (true) {
			time+=0.04f;
			contestant.update(0.04f);
			repaint();
			
			try {
				Thread.sleep(40);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	protected void paintComponent(Graphics g) {
	
		super.paintComponent(g);
		
		Graphics2D g2=Utils2D.createGraphics(g,true,false);
		
		// Paint background
		
		g2.setColor(Color.WHITE);
		
		for (int i=0; i<getWidth(); i+=50) {
			for (int j=0; j<getHeight(); j+=50) {
				g2.drawRect(i,j,50,50);
			}
		}
		
		g2.setColor(Color.ORANGE.brighter());
		g2.fillRect(0,0,100,getHeight());
		
		// Paint car
		
		float carX=transform.getPosition().x*SCALE;
		float carY=transform.getPosition().z*SCALE;
		float carAngle=-transform.getRotation().y-1.57f;
		float legendAngle=-contestant.getDirection()-1.57f;
		
		AffineTransform legendTransform=new AffineTransform();
		legendTransform.translate(carX,carY);
		legendTransform.rotate(legendAngle,legendImage.getWidth(null)/2,legendImage.getHeight(null)/2);
		g2.drawImage(legendImage,legendTransform,null);
		
		AffineTransform carTransform=new AffineTransform();
		carTransform.translate(carX,carY);
		carTransform.rotate(carAngle,carImage.getWidth(null)/2,carImage.getHeight(null)/2);
		g2.drawImage(carImage,carTransform,null);
		
		// Paint information
		
		g2.setColor(Color.BLACK);
		g2.drawString("Speed (ms): "+Utils.numberFormat(contestant.getSpeed(false),1),getWidth()-150,20);
		g2.drawString("Speed (kmh): "+Utils.numberFormat(contestant.getSpeed(true),1),getWidth()-150,40);
		g2.drawString("Angular: "+Utils.numberFormat(contestant.getAngular(),3),getWidth()-150,60);
		g2.drawString("Direction: "+Utils.numberFormat(contestant.getDirection(),2),getWidth()-150,80);
		g2.drawString("Orientation: "+Utils.numberFormat(contestant.getOrientation(),2),getWidth()-150,100);
		g2.drawString("Gear: "+contestant.getGear(),getWidth()-150,120);
		g2.drawString("RPM: "+contestant.getRPM(),getWidth()-150,140);
		g2.drawString("Grip: "+Utils.numberFormat(contestant.getGrip(),2),getWidth()-150,160);
	}
	
	
	
	private Contestant createContestant() {
		
		// Create car
		
		ICar car=new ICar() {
			public String getCarName() {
				return "test";
			}

			public String getInfo(String key) {
				return null;
			}

			public Matrix3D getTransform() {
				return transform;
			}

			public boolean isCollidingCar(float newX,float newY,float newZ,float newRot,ICar otherCar) {
				return false;
			}

			public boolean isCollidingObject(float newX,float newY,float newZ,float newRot) {
				return false;
			}

			public boolean isOnFloor() {
				return true;
			}

			public boolean isOnTrack() {
				return transform.getPosition().x>100f;
			}
		};
		
		// Create circuit
		
		ICircuit circuit=new ICircuit() {
			public String getCircuitName() {
				return "test";
			}

			public CircuitPoint getIntermediate(int index) {
				return new CircuitPoint(0f,0f,0f,0f,0f,true);
			}

			public CircuitPoint getPoint(int index) {
				return new CircuitPoint(0f,0f,0f,0f,0f,false);			
			}

			public CircuitPoint[] getPoints() {
				return new CircuitPoint[]{getPoint(0),getPoint(1),getPoint(2)};
			}
		};
		
		// Create contestant
		
		return new Contestant("test",session,car,circuit) {
			protected void updateControls(float dt) {
				setControl(Contestant.Controls.ACCELERATOR,controls[0]);
				setControl(Contestant.Controls.BRAKES,controls[1]);
				setControl(Contestant.Controls.STEERING,controls[3]-controls[2]);
			}			
		};
	}
	
	
	
	private Image createCarImage() {
	
		BufferedImage image=Utils2D.createImage(60,20);
		Graphics2D g2=image.createGraphics();
		g2.setColor(new Color(255,0,0));
		g2.fillRect(20,5,15,10);
		g2.drawLine(35,5,40,10);
		g2.drawLine(35,15,40,10);
		g2.dispose();
		
		return image;
	}
	
	
	
	private Image createLegendImage() {
		
		BufferedImage image=Utils2D.createImage(60,20);
		Graphics2D g2=image.createGraphics();
		g2.setColor(new Color(0,0,255,128));
		g2.drawLine(40,10,60,10);
		g2.dispose();
		
		return image;
	}
}