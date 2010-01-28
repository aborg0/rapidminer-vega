package com.rapidminer.repository.remote;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.SwingUtilities;

import com.rapid_i.repository.wsimport.AccessRights;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.RepositoryException;

/**
 * 
 * @author Simon Fischer
 *
 */
public class AccessRightsAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private RemoteEntry entry;

	public AccessRightsAction(RemoteEntry entry) {
		super("repository.edit_access_rights");
		this.entry = entry;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		new ProgressThread("download_from_repository") {
			@Override
			public void run() {
				
				try {
					final List<String> groupNames = entry.getRepository().getRepositoryService().getAllGroupNames();
					final List<AccessRights> accessRights = entry.getAccessRights();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							new AccessRightsDialog(entry, accessRights, groupNames).setVisible(true);			
						}
					});					
				} catch (RepositoryException e) {
					SwingTools.showSimpleErrorMessage("error_contacting_repository", e, e.getMessage());
				}					
			}
		}.start();		
	}
}
