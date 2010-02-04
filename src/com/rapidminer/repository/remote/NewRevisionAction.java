package com.rapidminer.repository.remote;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;

/**
 * 
 * @author Simon Fischer
 *
 */
public class NewRevisionAction extends ResourceAction {

	private static final long serialVersionUID = 1L;
	
	private RemoteProcessEntry entry;

	public NewRevisionAction(RemoteProcessEntry remoteProcessEntry) {
		super("repository.new_revision");
		this.entry = remoteProcessEntry;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			entry.getRepository().getRepositoryService().startNewRevision(entry.getPath());
			entry.getContainingFolder().refresh();
		} catch (Exception e1) {
			SwingTools.showSimpleErrorMessage("cannot_store_process_in_repository", e1, entry.getPath());
		}
	}
}
