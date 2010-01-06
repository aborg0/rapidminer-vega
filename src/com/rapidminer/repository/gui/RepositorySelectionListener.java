package com.rapidminer.repository.gui;

import java.util.EventListener;

/** Listens to the selection of repository entry in a  {@link RepositoryTree}. 
 * 
 * @author Simon Fischer
 *
 */
public interface RepositorySelectionListener extends EventListener {

	public void repositoryLocationSelected(RepositorySelectionEvent e);
	
}
