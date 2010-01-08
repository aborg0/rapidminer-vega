package com.rapidminer.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.event.HyperlinkListener;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.SettingsAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;

/** Convenience class for invoking certain actions from URLs, e.g. from inside a
 *  {@link HyperlinkListener}. URLs of the form "rm://actionName" will be interpreted.
 *  Actions can register themselves by invoking {@link RMUrlHandler#register(String, Action)}. 
 * 
 * @author Simon Fischer
 *
 */
public class RMUrlHandler {

	public static final String URL_PREFIX = "rm://";
	public static final String PREFERENCES_URL = URL_PREFIX + "preferences";
	
	private static final Map<String,Action> ACTION_MAP = new HashMap<String,Action>();
	
	static {
		register("preferences", new SettingsAction());
	}
	
	/**
	 * 
	 * @return true iff we understand the url.
	 */
	public static boolean handleUrl(String url) {		
		if (url.startsWith(URL_PREFIX)) {
			String suffix = url.substring(URL_PREFIX.length());
			if (suffix.startsWith("opdoc/")) {
				String opName = suffix.substring("opdoc/".length());
				Operator op;
				try {
					op = OperatorService.createOperator(opName);
					RapidMinerGUI.getMainFrame().getOperatorDocViewer().setDisplayedOperator(op);
				} catch (OperatorCreationException e) {
					LogService.getRoot().log(Level.WARNING, "Cannot create operator: "+opName, e);
				}
				return true;
			}
			Action action = ACTION_MAP.get(suffix);
			if (action != null) {
				action.actionPerformed(null);				
			} else {
				LogService.getRoot().warning("No action associated with URL "+url);
			}			
			return true; // we didn't make it, but noone else can, so we return true.
		} else {
			return false;
		}
	}

	public static void register(String name, Action action) {
		ACTION_MAP.put(name, action);
	}
}
