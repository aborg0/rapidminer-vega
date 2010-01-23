package com.rapidminer.gui.actions;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.net.URI;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;

/** Opens a browser.
 * 
 * @author Simon Fischer
 *
 */
public class BrowseAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private URI uri;

	public BrowseAction(String i18nKey, URI uri) {
		super(i18nKey);
		this.uri = uri;
		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			Desktop.getDesktop().browse(uri);
		} catch (Exception e1) {
			SwingTools.showSimpleErrorMessage("cannot_open_browser", e1);
		}
	}
}
