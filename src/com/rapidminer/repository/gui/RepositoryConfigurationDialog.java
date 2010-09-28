package com.rapidminer.repository.gui;

import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.repository.Repository;

/** Dialog to configure an existing repository.
 * 
 * @author Simon Fischer
 *
 */
public class RepositoryConfigurationDialog extends ButtonDialog {

	private static final long serialVersionUID = 1L;

	private RepositoryConfigurationPanel configurationPanel;
	private Repository repository;
	
	public RepositoryConfigurationDialog(Repository repository) {
		super("repositoryconfigdialog", true);
		this.repository = repository;
		configurationPanel = repository.makeConfigurationPanel();
		configurationPanel.configureUIElementsFrom(repository);

		layoutDefault(configurationPanel.getComponent(), DEFAULT_SIZE, makeCancelButton(), makeOkButton());
	}

	@Override
	protected void ok() {
		configurationPanel.configure(repository);
		super.ok();
	}
	
}
