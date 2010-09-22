/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.processeditor.profiler;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.StoreInRepositoryAction;
import com.rapidminer.gui.processeditor.profiler.data.ProfilerDataManager;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJToolBar;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.actions.ResetAllProfilerAction;
import com.rapidminer.gui.tools.actions.ResetProfilerAction;
import com.rapidminer.gui.tools.actions.SaveCumulativeProfilerAction;
import com.rapidminer.gui.tools.actions.SaveProfilerAction;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.gui.RepositoryLocationChooser;
import com.rapidminer.repository.local.SimpleIOObjectEntry;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * Profiler view, which shows statistics about the currently selected operator(s).
 * 
 * @author Marco Boeck
 */
public class ProfilingViewer extends JPanel implements Dockable, Observer {

	/** the JPanel within the JScrollPane */
	private JPanel innerPanel;
	
	/** the JPanel containing the toolbar buttons */
	private JPanel quickbarPanel;
	
	/** the ProfilingListener instance */
	private ProfilingListener listener;
	
	/** the constraints for the GridBag layout */
	private GridBagConstraints gbc;
	
	public transient final Action RESET_ALL_PROFILING_VIEWER_ACTION = new ResetAllProfilerAction(this);
	
	public transient final Action RESET_PROFILING_VIEWER_ACTION = new ResetProfilerAction(this);
	
	public transient final Action SAVE_PROFILING_VIEWER_ACTION = new SaveProfilerAction(this);
	
	public transient final Action SAVE_CUMULATIVE_PROFILING_VIEWER_ACTION = new SaveCumulativeProfilerAction(this);
	
	public static final String PROFILING_VIEWER_DOCK_KEY = "profiling_viewer";
	
	private static final Logger LOGGER = Logger.getLogger(ProfilingViewer.class.getName());
	
	private static final long serialVersionUID = 8710117784461815725L;
	
	private final DockKey DOCK_KEY = new ResourceDockKey(PROFILING_VIEWER_DOCK_KEY);
	
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}
	
	
	/**
	 * Constructor which needs the ProfilingListener instance.
	 * @param listener the ProfilingListener instance registered in the MainFrame
	 */
	public ProfilingViewer(ProfilingListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		this.listener = listener;
		listener.addObserver(this);
		
		quickbarPanel = new JPanel();
		quickbarPanel.setLayout(new GridBagLayout());
		JToolBar toolBar = new ExtendedJToolBar();
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        toolBar.add(RESET_PROFILING_VIEWER_ACTION);
        toolBar.add(RESET_ALL_PROFILING_VIEWER_ACTION);
        toolBar.add(SAVE_PROFILING_VIEWER_ACTION);
        toolBar.add(SAVE_CUMULATIVE_PROFILING_VIEWER_ACTION);
        JToggleButton toggleMerge = new JToggleButton(new ResourceAction(true, "merge_profiling_toggle_viewer") {

			private static final long serialVersionUID = -3583260287309226987L;

			@Override
    		public void actionPerformed(ActionEvent e) {
    			ProfilingViewer.this.listener.toggleAutoMerge();
    			if (ProfilerDataManager.getInstance().getMergeLocation() == null && ProfilingViewer.this.listener.isAutoMergeEnabled()) {
					String loc = RepositoryLocationChooser.selectLocation(null, null, RapidMinerGUI.getMainFrame(), true, false);
					if (loc != null) {
						RepositoryLocation location;
						try {
							location = new RepositoryLocation(loc);
							ProfilerDataManager.getInstance().setMergeLocation(location);
						} catch (Exception ex) {
							SwingTools.showSimpleErrorMessage("malformed_rep_location", ex, loc);
						}
					} else {
						// location choosing cancelled
						ProfilingViewer.this.listener.toggleAutoMerge();
						((JToggleButton)e.getSource()).setSelected(false);
					}
				}
    		}	
    	});
        toggleMerge.setText(null);
        toggleMerge.setSelected(false);
        toolBar.add(toggleMerge);
        JToggleButton disableProfiler = new JToggleButton(new ResourceAction(true, "profiling_viewer_enable") {

			private static final long serialVersionUID = -3583260287309226987L;

			@Override
    		public void actionPerformed(ActionEvent e) {
    			ProfilingViewer.this.listener.toggleProfiling();
    		}	
    	});
        disableProfiler.setText(null);
        disableProfiler.setSelected(false);
        toolBar.add(disableProfiler);
        
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 1.0f;
		quickbarPanel.add(toolBar, gbc);
		
		innerPanel = new JPanel();
		innerPanel.setDoubleBuffered(true);
		innerPanel.setLayout(new GridBagLayout());
		ExtendedJScrollPane scrollPane = new ExtendedJScrollPane(innerPanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		this.setDoubleBuffered(true);
		this.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		
		// format GridBagLayout
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.ipadx = 2;
		gbc.ipady = 0;
		gbc.weightx = 1.0f;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		this.add(quickbarPanel, gbc);
		
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1.0f;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.ipadx = 2;
		gbc.ipady = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		this.add(scrollPane, gbc);
	}
	
	/**
	 * Updates the GUI.
	 */
	private void updateGui() {
		// remove old components
		innerPanel.removeAll();
		innerPanel.repaint();
		gbc = new GridBagConstraints();
		
		// format GridBagLayout// format GridBagLayout
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0f;
		gbc.weighty = 0.0f;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(0, 0, 4, 0);
		int y = 0;
		// add new components depending on what is selected
		if (listener.getSelectedOperatorList().get(0) instanceof ProcessRootOperator) {
			for (Operator op : listener.getCurrentProcess().getAllOperators()) {
				gbc.gridx = 0;
				gbc.gridy = y++;
				innerPanel.add(new OperatorPanel(op), gbc);
			}
		} else {
			for (Operator op : listener.getSelectedOperatorList()) {
				gbc.gridx = 0;
				gbc.gridy = y++;
				innerPanel.add(new OperatorPanel(op), gbc);
			}
//		RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().
//			getDisplayedChain().getAllInnerOperators();
		}
		// add dummy element so real elements appear at the top of the panel
		gbc.weighty = 1.0f;
		innerPanel.add(new JLabel(), gbc);
		// update the panel immediately
		innerPanel.revalidate();
		this.revalidate();
	}
	
	/**
	 * Clears all existing profiling data.
	 */
	public void resetAll() {
		LOGGER.info("Cleared all profiler data.");
		ProfilerDataManager.getInstance().resetAllData();
		update(null, null);
	}
	
	/**
	 * Clears the existing profiling data of the current process.
	 */
	public void resetCurrent() {
		LOGGER.info("Cleared current process profiling data.");
		ProfilerDataManager.getInstance().resetCurrentData();
		update(null, null);
	}
	
	/**
	 * Saves the existing profiling data as an ExampleSet.
	 */
	public void saveData() {
		ExampleSet example = ProfilerDataManager.getInstance().getProfilingDataAsExampleSet();
		new StoreInRepositoryAction(example).actionPerformed(null);
		LOGGER.info("Saving profiling data.");
	}
	
	/**
	 * Merges the existing profiling data with existing profiling data from the specified ExampleSet.
	 */
	public void mergeData() {
		SwingUtilities.invokeLater(new Runnable(){

			@Override
			public void run() {
				ExampleSet existingExample;
				String loc = RepositoryLocationChooser.selectLocation(null, null, RapidMinerGUI.getMainFrame(), true, false);
				if (loc != null) {
					RepositoryLocation location;
					try {
						location = new RepositoryLocation(loc);
					} catch (Exception ex) {
						SwingTools.showSimpleErrorMessage("malformed_rep_location", ex, loc);
						return;
					}
					try {
						SimpleIOObjectEntry entry = (SimpleIOObjectEntry)location.locateEntry();
						// entry is null if new ExampleSet has been chosen in den wizard
						ExampleSet example;
						if (entry != null) {
							IOObject iooObject = entry.retrieveData(null);
							existingExample = (ExampleSet)iooObject;
							example = ProfilerDataManager.getInstance().getProfilingDataAsMergedExampleSet(existingExample);
						} else {
							example = ProfilerDataManager.getInstance().getProfilingDataAsMergedExampleSet(null);
						}
						RepositoryManager.getInstance(null).store(example, location, null);
					} catch (RepositoryException ex) {
						SwingTools.showSimpleErrorMessage("cannot_store_obj_at_location", ex, loc);
					}			
				}
			}
			
		});
		LOGGER.info("Merging profiling data.");
	}
	
	/**
	 * Automatically merges the existing profiling data with existing profiling data from the specified ExampleSet.
	 */
	private void mergeAutomaticallyData(RepositoryLocation location) {
		ExampleSet existingExample;
		try {
			SimpleIOObjectEntry entry = (SimpleIOObjectEntry)location.locateEntry();
			// entry is null if new ExampleSet has been chosen in den wizard
			ExampleSet example;
			if (entry != null) {
				IOObject iooObject = entry.retrieveData(null);
				existingExample = (ExampleSet)iooObject;
				example = ProfilerDataManager.getInstance().getProfilingDataAsMergedExampleSet(existingExample);
			} else {
				example = ProfilerDataManager.getInstance().getProfilingDataAsMergedExampleSet(null);
			}
			RepositoryManager.getInstance(null).store(example, location, null);
		} catch (RepositoryException ex) {
			SwingTools.showSimpleErrorMessage("cannot_store_obj_at_location", ex, location.getPath());
		}			
		LOGGER.info("Auto-Merging profiling data.");
	}
	
	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	@Override
	public void update(Observable o, Object arg) {
		// if true, a process has finished execution
		if (Boolean.parseBoolean(String.valueOf(arg))) {
			// if auto merge is enabled
			if (ProfilingViewer.this.listener.isAutoMergeEnabled()) {
				mergeAutomaticallyData(ProfilerDataManager.getInstance().getMergeLocation());
				resetCurrent();
			}
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				updateGui();
			}
			
		});
	}

}
