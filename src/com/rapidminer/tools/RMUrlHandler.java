package com.rapidminer.tools;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.event.HyperlinkListener;

import com.rapidminer.gui.actions.SettingsAction;

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
	
	public static boolean handleUrl(String url) {		
		if (url.startsWith(URL_PREFIX)) {
			String suffix = url.substring(URL_PREFIX.length());
			Action action = ACTION_MAP.get(suffix);
			if (action != null) {
				action.actionPerformed(null);
				return true;
			} else {
				LogService.getRoot().warning("No action associated with URL "+url);
				return false;
			}			
		} else {
			return false;
		}
	}

	public static void register(String name, Action action) {
		ACTION_MAP.put(name, action);
	}
}
