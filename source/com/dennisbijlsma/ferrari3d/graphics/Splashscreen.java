//-----------------------------------------------------------------------------
// Ferrari3D
// SplashScreen
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Image;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;

/**
 * Shows a dialog window with a progress bar while some operation is being
 * performed. Splash screens can be shown either while the application is being
 * loaded, or when a large blocking operation is being performed.
 */
public class Splashscreen extends JPanel {
	
	private JFrame window;
	private Image logoImage;
	private JProgressBar progressBar;
	
	private int progress;
	private String message;
	
	private static final int WINDOW_WIDTH = 500;
	private static final int WINDOW_HEIGHT = 200;
	private static final Color BACKGROUND_COLOR_1 = new Color(0, 100, 255);
	private static final Color BACKGROUND_COLOR_2 = new Color(0, 100, 200);
	private static final Color BORDER_COLOR = new Color(255, 255, 255);
	private static final Color MESSAGE_COLOR = new Color(255, 255, 255);
	private static final Font MESSAGE_FONT = new Font("Verdana", Font.BOLD, 12);
	
	public Splashscreen() {
		super(null);
		logoImage = Utils.loadImage("data/graphics/logo.png");
		progress = 0;
		message = Settings.getInstance().getText("game.loading");
	}
	
	public void showSplashScreen() {
		
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);

		window = new JFrame("");
		window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		window.setLocationRelativeTo(null);
		window.setResizable(false);
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setUndecorated(true);
		window.setAlwaysOnTop(true);
		window.setLayout(new BorderLayout());
		window.add(this, BorderLayout.CENTER);
		window.add(progressBar, BorderLayout.SOUTH);
		window.setVisible(true);
	}

	public void hideSplashScreen() {
		window.dispose();
	}
	
	@Override
	protected void paintComponent(Graphics g) {
	
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D) g;
		
		// Paint background
		
		g2.setPaint(new GradientPaint(0, 0, BACKGROUND_COLOR_1, 0, getHeight(), BACKGROUND_COLOR_2));
		g2.fillRect(0, 0, getWidth(), getHeight());
		g2.drawImage(logoImage, getWidth() / 2 - logoImage.getWidth(null) / 2, 20, null);
		g2.setColor(BORDER_COLOR);
		g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		
		// Paint message
		
		g2.setFont(MESSAGE_FONT);
		g2.setColor(MESSAGE_COLOR);
		g2.drawString(message, 20,getHeight() - 20);
	}
	
	public void setProgress(int progress) {
		this.progress = progress;
		progressBar.setValue(progress);
	}
}
