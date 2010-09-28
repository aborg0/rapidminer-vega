package com.rapidminer.repository.gui;

import java.awt.Component;

import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryManager;

/** Panel to configure a repository.
 * 
 * @author Simon Fischer
 *
 */
public interface RepositoryConfigurationPanel {

	/** (Asynchronously) creates a new repository and adds it to the {@link RepositoryManager}. */
	public void makeRepository();

	/** Configures the UI elements to show the properties defined by the given repository. */
	public void configureUIElementsFrom(Repository repository);
	
	/** Configures given repository with the values entered into the dialog. 
	 * @return true if configuration is ok */
	public boolean configure(Repository repository);
	
	/** Returns the actual component. */
	public Component getComponent();

}
