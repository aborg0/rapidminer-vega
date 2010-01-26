package com.rapidminer.repository.gui.process;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTree;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

/**
 * 
 * @author Simon Fischer
 *
 */
public class RemoteProcessViewer extends JPanel implements Dockable {

	private static final long serialVersionUID = 1L;
	
	public RemoteProcessViewer() {
		setLayout(new BorderLayout());
		JTree tree = new JTree(new RemoteProcessesTreeModel());
		tree.setCellRenderer(new RemoteProcessTreeCellRenderer());
		tree.setShowsRootHandles(true);
		tree.setRootVisible(false);
		add(tree, BorderLayout.CENTER);
	}
	
	
	public static final String PROCESS_PANEL_DOCK_KEY = "remote_process_viewer";
	private final DockKey DOCK_KEY = new ResourceDockKey(PROCESS_PANEL_DOCK_KEY);
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

}
