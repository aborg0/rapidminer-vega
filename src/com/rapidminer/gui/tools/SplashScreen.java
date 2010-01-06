/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;


/**
 * The splash screen is displayed during start up of RapidMiner. It displays the
 * logo and the some start information. The product logo should have a size of 
 * approximately 270 times 70 pixels.
 * 
 * @author Ingo Mierswa
 */
public class SplashScreen extends JPanel {

	private static final long serialVersionUID = -1525644776910410809L;

	private static final Paint MAIN_PAINT = Color.BLACK;
	
	private static Image backgroundImage = null;
	
	
	private static final int MARGIN = 10;
	
	private static final String PROPERTY_FILE = "splash_infos.properties";
	
	static {
		try {			
			URL url = Tools.getResource("splashscreen_community.png");
			if (url != null)
				backgroundImage = ImageIO.read(url);
		} catch (IOException e) {
			LogService.getGlobal().logWarning("Cannot load images for splash screen. Using empty splash screen...");
		}
	}

	
	private transient Image productLogo;
	
	private Properties properties;
	
	private JFrame splashScreenFrame = new JFrame();
	
	private String message = "Starting...";

	private boolean infosVisible;
	
	public SplashScreen(String productVersion, Image productLogo) {
		this(productLogo, createDefaultProperties(productVersion));
	}
	
	public SplashScreen(String productVersion, Image productLogo, URL propertyFile) {
		this(productLogo, createProperties(productVersion, propertyFile));
	}
	
	public SplashScreen(Image productLogo, Properties properties) {
		super();
		
		this.properties = properties;
		
		this.productLogo = productLogo;
		
		splashScreenFrame = new JFrame(properties.getProperty("name"));
		splashScreenFrame.getContentPane().add(this);
		SwingTools.setFrameIcon(splashScreenFrame);

		splashScreenFrame.setUndecorated(true);
		if (backgroundImage != null)
			splashScreenFrame.setSize(backgroundImage.getWidth(this), backgroundImage.getHeight(this));
		else
			splashScreenFrame.setSize(450, 350);
		splashScreenFrame.setLocationRelativeTo(null);
	}

	private static Properties createDefaultProperties(String productVersion) {
		return createProperties(productVersion, Tools.getResource(PROPERTY_FILE));
	}
	
	private static Properties createProperties(String productVersion, URL propertyFile) {
		Properties properties = new Properties();
		if (propertyFile != null) {
			try {
				InputStream in = propertyFile.openStream();
				properties.load(in);
				in.close();
			} catch (Exception e) {
				LogService.getGlobal().logError("Cannot read splash screen infos: " + e.getMessage());
			}
		}
		properties.setProperty("version", productVersion);
		return properties;
	}
	
	public void showSplashScreen() {
		splashScreenFrame.setVisible(true);
	}

	public JFrame getSplashScreenFrame() {
		return splashScreenFrame;
	}
	
	public void dispose() {
		splashScreenFrame.dispose();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		drawMain((Graphics2D)g);
		g.setColor(Color.black);
		g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
	}

	public void drawMain(Graphics2D g) {
		g.setPaint(MAIN_PAINT);
		g.fillRect(0, 0, getWidth(), getHeight());

		if (backgroundImage != null)
			g.drawImage(backgroundImage, 0, 0, this);

		if (productLogo != null)
			g.drawImage(productLogo, getWidth() / 2 - productLogo.getWidth(this) / 2, 90, this);
		
		g.setColor(SwingTools.BROWN_FONT_COLOR);
		if (message != null) {
			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
			drawString(g, message, 255);
		}
		
		if (infosVisible) {
			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
			drawString(g, properties.getProperty("name") + " " + properties.getProperty("version"), 275);
			drawString(g, properties.getProperty("license"), 290);
			drawString(g, properties.getProperty("warranty"), 305);
			drawString(g, properties.getProperty("copyright"), 320);
			drawString(g, properties.getProperty("more"), 335);
		}
	}

	private void drawString(Graphics2D g, String text, int height) {
		if (text == null)
			return;
//		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(text, g);
//		float xPos = (float)(getWidth() - MARGIN - stringBounds.getWidth());
		float xPos = MARGIN;
		float yPos = height;
		g.drawString(text, xPos, yPos);
	}
	
	public void setMessage(String message) {
		this.message = message;
		repaintLater();
	}
	
	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
		repaintLater();
	}

	public void setInfosVisible(boolean b) {
		this.infosVisible = b;
		repaintLater();
	}

	private void repaintLater() {
		if (SwingUtilities.isEventDispatchThread()) {
			repaint();
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					repaint();					
				}				
			});
		}		
	}	
}
