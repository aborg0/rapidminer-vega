/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import com.rapidminer.gui.OperatorDocViewer;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;

public class ShowHelpTextInBrowserAction extends ResourceAction {

	private final OperatorDocViewer operatorDocViewer;
	private static final String[] browsers = new String[] { "iexplorer" };
	public static final String WIKI_PREFIX_FOR_OPERATORS = "http://rapid-i.com/wiki/index.php?title=";

	public ShowHelpTextInBrowserAction(boolean smallIcon, String i18nKey, Object[] i18nArgs, OperatorDocViewer operatorDocViewer) {
		super(smallIcon, i18nKey, i18nArgs);
		this.operatorDocViewer = operatorDocViewer;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 909390054503086861L;

	@Override
	public void actionPerformed(ActionEvent e) {
		String url = WIKI_PREFIX_FOR_OPERATORS + this.operatorDocViewer.getDisplayedOperatorDescName().replaceAll(" ", "_");
		try {
			// Attempt to use Desktop library from JDK 1.6+
			Class<?> d = Class.forName("java.awt.Desktop");
			d.getDeclaredMethod("browse", new Class[] { java.net.URI.class }).invoke(d.getDeclaredMethod("getDesktop").invoke(null), new Object[] { java.net.URI.create(url) });
		} catch (Exception ignore) {
			// Library not available or failed
			String osName = System.getProperty("os.name");
			if (osName.startsWith("Mac")) {
				try {
					Class.forName("com.apple.eio.FileManager").getDeclaredMethod("openURL", new Class[] { String.class }).invoke(null, new Object[] { url });
				} catch (IllegalArgumentException e1) {
					SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", null, true, url);
				} catch (SecurityException e1) {
					SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", null, true, url);
				} catch (IllegalAccessException e1) {
					SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", null, true, url);
				} catch (InvocationTargetException e1) {
					SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", null, true, url);
				} catch (NoSuchMethodException e1) {
					SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", null, true, url);
				} catch (ClassNotFoundException e1) {
					SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", null, true, url);
				}
			} else if (osName.startsWith("Windows")) {
				try {
					Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
					SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", null, true, url);
				} catch (IOException e1) {
					SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", e1, true, url);
				}
				// new ProcessBuilder( "rundll32", "url.dll,FileProtocolHandler", url ).start();
			} else {
				// Assume Unix or Linux
				String browser = null;
				for (String b : browsers) {
					try {
						if (browser == null && Runtime.getRuntime().exec(new String[] { "which", b }).getInputStream().read() != -1) {
							Runtime.getRuntime().exec(new String[] { browser = b, url });
							if (browser == null) {
								SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", null, true, Arrays.toString(browsers));
							}
						}
					} catch (IOException e1) {
						SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", e1, true, url);
					} catch (Exception e1) {
						SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", e1, true, url);
					}
				}
			}
		}
	}
}
