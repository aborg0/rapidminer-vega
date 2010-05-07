package com.rapidminer.gui.tools;

import java.awt.event.ActionEvent;
import java.util.logging.Level;

import javax.swing.AbstractAction;

/**
 * 
 * @author Simon Fischer
 *
 */
public class LoggingLevelMenu extends ResourceMenu {

	private static final long serialVersionUID = 1L;

	public LoggingLevelMenu(final LoggingViewer viewer) {
		super("log_level");
		for (final Level level : LoggingViewer.SELECTABLE_LEVELS) {
			add(new AbstractAction(level.getName()) {				
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					viewer.setLevel(level);
				}
			});
		}
	}
}
