/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2012 by Rapid-I and the contributors
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
package com.rapidminer.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.actions.Actions;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.actions.RunAction;
import com.rapidminer.gui.actions.SaveAction;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.operatortree.OperatorTree;
import com.rapidminer.gui.processeditor.NewOperatorEditor;
import com.rapidminer.gui.processeditor.ProcessContextProcessEditor;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.processeditor.results.DockableResultDisplay;
import com.rapidminer.gui.processeditor.results.ResultDisplay;
import com.rapidminer.gui.processeditor.results.TabbedResultDisplay;
import com.rapidminer.gui.properties.OperatorPropertyPanel;
import com.rapidminer.gui.tools.LoggingViewer;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.vlsolutions.swing.docking.DockGroup;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockingDesktop;

/**
 * The main component class of the RapidMiner GUI. The class holds a lot of Actions
 * that can be used for the tool bar and for the menu bar. MainFrame has methods
 * for handling the process (saving, opening, creating new). It keeps track
 * of the state of the process and enables/disables buttons. It must be
 * notified whenever the process changes and propagates this event to its
 * children. Most of the code is enclosed within the Actions.
 * 
 * @author Ingo Mierswa, Simon Fischer, Sebastian Land
 */
public class MainFrame extends ApplicationFrame implements WindowListener, MainUIState, ProcessEndHandler {

    /** The property name for &quot;The pixel size of each plot in matrix plots.&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_MATRIXPLOT_SIZE = "rapidminer.gui.plotter.matrixplot.size";

    /**
     * The property name for &quot;The maximum number of rows used for a plotter, using only a sample of this size if more rows are
     * available.&quot;
     */
    public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_ROWS_MAXIMUM = "rapidminer.gui.plotter.rows.maximum";

    /** The property name for &quot;Limit number of displayed classes plotter legends. -1 for no limit.&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_CLASSLIMIT = "rapidminer.gui.plotter.legend.classlimit";

    /** The property name for &quot;The color for minimum values of the plotter legend.&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MINCOLOR = "rapidminer.gui.plotter.legend.mincolor";

    /** The property name for &quot;The color for maximum values of the plotter legend.&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MAXCOLOR = "rapidminer.gui.plotter.legend.maxcolor";

    /** The property name for &quot;Limit number of displayed classes for colorized plots. -1 for no limit.&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_COLORS_CLASSLIMIT = "rapidminer.gui.plotter.colors.classlimit";

    /** The property name for &quot;Maximum number of states in the undo list.&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_UNDOLIST_SIZE = "rapidminer.gui.undolist.size";

    /** The property name for &quot;Maximum number of examples to use for the attribute editor. -1 for no limit.&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_ATTRIBUTEEDITOR_ROWLIMIT = "rapidminer.gui.attributeeditor.rowlimit";

    /** The property name for &quot;Beep on process success?&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_BEEP_SUCCESS = "rapidminer.gui.beep.success";

    /** The property name for &quot;Beep on error?&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_BEEP_ERROR = "rapidminer.gui.beep.error";

    /** The property name for &quot;Beep when breakpoint reached?&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_BEEP_BREAKPOINT = "rapidminer.gui.beep.breakpoint";

    /** The property name for &quot;Limit number of displayed rows in the message viewer. -1 for no limit.&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_ROWLIMIT = "rapidminer.gui.messageviewer.rowlimit";

    /** The property name for &quot;The color for notes in the message viewer.&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_HIGHLIGHT_NOTES = "rapidminer.gui.messageviewer.highlight.notes";

    /** The property name for &quot;The color for warnings in the message viewer.&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_HIGHLIGHT_WARNINGS = "rapidminer.gui.messageviewer.highlight.warnings";

    /** The property name for &quot;The color for errors in the message viewer.&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_HIGHLIGHT_ERRORS = "rapidminer.gui.messageviewer.highlight.errors";

    /** The property name for &quot;The color for the logging service indicator in the message viewer.&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_HIGHLIGHT_LOGSERVICE = "rapidminer.gui.messageviewer.highlight.logservice";

    /** The property name for &quot;Shows process info screen after loading?&quot; */
    public static final String PROPERTY_RAPIDMINER_GUI_PROCESSINFO_SHOW = "rapidminer.gui.processinfo.show";

    public static final String PROPERTY_RAPIDMINER_GUI_SAVE_BEFORE_RUN = "rapidminer.gui.save_before_run";

    public static final String PROPERTY_RAPIDMINER_GUI_SAVE_ON_PROCESS_CREATION = "rapidminer.gui.save_on_process_creation";

    /** The property determining whether or not to switch to result view when results are produced. */
    public static final String PROPERTY_RAPIDMINER_GUI_AUTO_SWITCH_TO_RESULTVIEW = "rapidminer.gui.auto_switch_to_resultview";

    /** Determines whether we build a {@link TabbedResultDisplay} or a {@link DockableResultDisplay}. */
    public static final String PROPERTY_RAPIDMINER_GUI_RESULT_DISPLAY_TYPE = "rapidminer.gui.result_display_type";

    /** Log level of the LoggingViewer. */
    public static final String PROPERTY_RAPIDMINER_GUI_LOG_LEVEL = "rapidminer.gui.log_level";

    private static final long serialVersionUID = -1602076945350148969L;

	private final AbstractUIState state;

	public static final DockGroup DOCK_GROUP_ROOT = AbstractUIState.DOCK_GROUP_ROOT;
	public static final DockGroup DOCK_GROUP_RESULTS = AbstractUIState.DOCK_GROUP_RESULTS;

    // --------------------------------------------------------------------------------

    

    // public static final String EDIT_MODE_NAME = "edit";
    // public static final String RESULTS_MODE_NAME = "results";
    // public static final String WELCOME_MODE_NAME = "welcome";

    

    // --------------------------------------------------------------------------------

    // DOCKING

    

    // --------------------------------------------------------------------------------
    // LISTENERS And OBSERVERS

    

    // --------------------------------------------------------------------------------

    /** Creates a new main frame containing the RapidMiner GUI. */
    public MainFrame() {
        this("welcome");
    }

    public MainFrame(String initialPerspective) {
        super(AbstractUIState.TITLE);
        state = new AbstractUIState(initialPerspective, this, getContentPane()) {
			@Override
			public boolean close() {
				if (changed) {
					final ProcessLocation loc = process.getProcessLocation();
					String locName;
					if (loc != null) {
						locName = loc.getShortName();
					} else {
						locName = "unnamed";
					}
					switch (SwingTools.showConfirmDialog("save",
							ConfirmDialog.YES_NO_CANCEL_OPTION, locName)) {
					case ConfirmDialog.YES_OPTION:
						SaveAction.save(getProcess());

						// it may happen that save() does not actually save the process,
						// because the user hits cancel in the
						// saveAs dialog or an error occurs. In this case the process
						// won't be marked as unchanged. Thus,
						// we return the process changed status.
						return !isChanged();
					case ConfirmDialog.NO_OPTION:
						if (getProcessState() != Process.PROCESS_STATE_STOPPED) {
							synchronized (processThread) {
								processThread.stopProcess();
							}
						}
						return true;
					default: // cancel
						return false;
					}
				} else {
					return true;
				}
			}

			@Override
			public void exit(boolean relaunch) {
				if (changed) {
					final ProcessLocation loc = process.getProcessLocation();
					String locName;
					if (loc != null) {
						locName = loc.getShortName();
					} else {
						locName = "unnamed";
					}
					switch (SwingTools.showConfirmDialog("save",
							ConfirmDialog.YES_NO_CANCEL_OPTION, locName)) {
					case ConfirmDialog.YES_OPTION:
						SaveAction.save(process);
						if (changed) {
							return;
						}
						break;
					case ConfirmDialog.NO_OPTION:
						break;
					case ConfirmDialog.CANCEL_OPTION:
					default:
						return;
					}
				} else {
					if (!relaunch) { // in this case we have already confirmed
						final int answer = ConfirmDialog.showConfirmDialog("exit",
								ConfirmDialog.YES_NO_OPTION,
								RapidMinerGUI.PROPERTY_CONFIRM_EXIT,
								ConfirmDialog.YES_OPTION);
						if (answer != ConfirmDialog.YES_OPTION) {
							return;
						}
					}
				}
				stopProcess();
				dispose();
				RapidMiner.quit(relaunch ? RapidMiner.ExitMode.RELAUNCH
						: RapidMiner.ExitMode.NORMAL);
			}

			@Override
			public void updateRecentFileList() {
		        recentFilesMenu.removeAll();
		        List<ProcessLocation> recentFiles = RapidMinerGUI.getRecentFiles();
		        int j = 1;
		        for (final ProcessLocation recentLocation : recentFiles) {
		            JMenuItem menuItem = new JMenuItem(j + " " + recentLocation.toMenuString());
		            menuItem.setMnemonic('0' + j);
		            menuItem.addActionListener(new ActionListener() {
		                @Override
		                public void actionPerformed(ActionEvent e) {
		    				if (RapidMinerGUI.getMainFrame().close()){
		    					OpenAction.open(recentLocation, true);
		    				}
		                }
		            });
		            recentFilesMenu.add(menuItem);
		            j++;
		        }
			}

			@Override
			protected void setTitle() {
		        if (hostname == null) {
		            try {
		                hostname = "@" + InetAddress.getLocalHost().getHostName();
		            } catch (UnknownHostException e) {
		                hostname = "";
		            }
		        }

		        if (this.process != null) {
		            ProcessLocation loc = process.getProcessLocation();
		            if (loc != null) {
		                MainFrame.this.setTitle(loc.getShortName() + (changed ? "*" : "") + " \u2013 " + TITLE + hostname);
		            } else {
		            	MainFrame.this.setTitle("<new process"    + (changed ? "*" : "") + "> \u2013 " + TITLE + hostname);
		            }
		        } else {
		        	MainFrame.this.setTitle(TITLE + hostname);
		        }
			}};
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(this);
        pack();
        //state.metaDataUpdateQueue.start();
    }

    /**
     * 
     * @deprecated Use {@link #getPerspectives()} and {@link Perspectives#showPerspective(String)}
     */
	@Deprecated
    public void changeMode(int mode) {
        // TODO: remove
    }

    /**
     * @deprecated Use {@link #getProcess()} instead
     */
	@Deprecated
    public final Process getExperiment() {
        return getProcess();
    }

    

    // ====================================================
    // M A I N A C T I O N S
    // ===================================================

    /**
     * Sets a new process and registers the MainFrame listener. Please note
     * that this method does not invoke {@link #processChanged()}. Do so
     * if necessary.
     * 
     * @deprecated Use {@link #setProcess(Process, boolean)} instead
     */
	@Deprecated
    public void setExperiment(Process process) {
        setProcess(process, true);
    }

    /**
     * Must be called when the process changed (such that is different from
     * the process before). Enables the correct actions if the process
     * can be saved to disk.
     * 
     * @deprecated this method is no longer necessary (and does nothing) since the MainFrame
     *             observes the process using an Observer pattern. See {@link #processObserver}.
     */
	@Deprecated
    public void processChanged() {
    }

    /**
     * Sets the window title (RapidMiner + filename + an asterisk if process was
     * modified.
     */
	protected void setTitle() {
    	state.setTitle();
    }

    // //////////////////// File menu actions ////////////////////

    /* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#updateRecentFileList()
	 */
	public void updateRecentFileList() {
		state.updateRecentFileList();
    }

	@Override
	public boolean close() {
		return state.close();
	}

	@Override
	public void exit(final boolean relaunch) {
		state.exit(relaunch);
	}

	@Override
    public void windowOpened(WindowEvent e) {
    }

	@Override
    public void windowClosing(WindowEvent e) {
        exit(false);
    }

	@Override
    public void windowClosed(WindowEvent e) {
    }

	@Override
    public void windowIconified(WindowEvent e) {
    }

	@Override
    public void windowDeiconified(WindowEvent e) {
    }

	@Override
    public void windowActivated(WindowEvent e) {
    }

	@Override
    public void windowDeactivated(WindowEvent e) {
    }

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getWindow()
	 */
	@Override
	public JFrame getWindow() {
		return this;
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MenusUI#addMenuItem(int, int, javax.swing.JMenuItem)
	 */
	@Override
	public void addMenuItem(int menuIndex, int itemIndex, JMenuItem item) {
		state.addMenuItem(menuIndex, itemIndex, item);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MenusUI#addMenu(int, javax.swing.JMenu)
	 */
	@Override
	public void addMenu(int menuIndex, JMenu menu) {
		state.addMenu(menuIndex, menu);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MenusUI#addMenuSeparator(int)
	 */
	@Override
	public void addMenuSeparator(int menuIndex) {
		state.addMenuSeparator(menuIndex);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MenusUI#removeMenu(int)
	 */
	@Override
	public void removeMenu(int index) {
		state.removeMenu(index);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MenusUI#removeMenuItem(int, int)
	 */
	@Override
	public void removeMenuItem(int menuIndex, int itemIndex) {
		state.removeMenuItem(menuIndex, itemIndex);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MenusUI#getFileMenu()
	 */
	@Override
	public JMenu getFileMenu() {
		return state.getFileMenu();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MenusUI#getToolsMenu()
	 */
	@Override
	public JMenu getToolsMenu() {
		return state.getToolsMenu();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MenusUI#getMainMenuBar()
	 */
	@Override
	public JMenuBar getMainMenuBar() {
		return state.getMainMenuBar();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MenusUI#getEditMenu()
	 */
	@Override
	public JMenu getEditMenu() {
		return state.getEditMenu();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MenusUI#getProcessMenu()
	 */
	@Override
	public JMenu getProcessMenu() {
		return state.getProcessMenu();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MenusUI#getHelpMenu()
	 */
	@Override
	public JMenu getHelpMenu() {
		return state.getHelpMenu();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.TutorialState#startTutorial()
	 */
	@Override
	public void startTutorial() {
		state.startTutorial();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.TutorialState#setTutorialMode(boolean)
	 */
	@Override
	public void setTutorialMode(boolean mode) {
		state.setTutorialMode(mode);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.TutorialState#isTutorialMode()
	 */
	@Override
	public boolean isTutorialMode() {
		return state.isTutorialMode();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#validateProcess(boolean)
	 */
	@Override
	public void validateProcess(boolean force) {
		state.validateProcess(force);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#getProcessState()
	 */
	@Override
	public int getProcessState() {
		return state.getProcessState();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#getProcess()
	 */
	@Override
	public Process getProcess() {
		return state.getProcess();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#newProcess()
	 */
	@Override
	public void newProcess() {
		state.newProcess();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#runProcess()
	 */
	@Override
	public void runProcess() {
		state.runProcess();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#stopProcess()
	 */
	@Override
	public void stopProcess() {
		state.stopProcess();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#pauseProcess()
	 */
	@Override
	public void pauseProcess() {
		state.pauseProcess();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#setProcess(com.rapidminer.Process, boolean)
	 */
	@Override
	public void setProcess(Process process, boolean newProcess) {
		state.setProcess(process, newProcess);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#isChanged()
	 */
	@Override
	public boolean isChanged() {
		return state.isChanged();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#undo()
	 */
	@Override
	public void undo() {
		state.undo();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#redo()
	 */
	@Override
	public void redo() {
		state.redo();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#setOpenedProcess(com.rapidminer.Process, boolean, java.lang.String)
	 */
	@Override
	public void setOpenedProcess(Process process, boolean showInfo,
			String sourceName) {
		state.setOpenedProcess(process, showInfo, sourceName);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#saveAsTemplate()
	 */
	@Override
	public void saveAsTemplate() {
		state.saveAsTemplate();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#fireProcessUpdated()
	 */
	@Override
	public void fireProcessUpdated() {
		state.fireProcessUpdated();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessState#processHasBeenSaved()
	 */
	@Override
	public void processHasBeenSaved() {
		state.processHasBeenSaved();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#setExpertMode(boolean)
	 */
	@Override
	public void setExpertMode(boolean expert) {
		state.setExpertMode(expert);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getPropertyPanel()
	 */
	@Override
	public OperatorPropertyPanel getPropertyPanel() {
		return state.getPropertyPanel();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getMessageViewer()
	 */
	@Override
	public LoggingViewer getMessageViewer() {
		return state.getMessageViewer();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getNewOperatorEditor()
	 */
	@Override
	public NewOperatorEditor getNewOperatorEditor() {
		return state.getNewOperatorEditor();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getOperatorTree()
	 */
	@Override
	public OperatorTree getOperatorTree() {
		return state.getOperatorTree();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getActions()
	 */
	@Override
	public Actions getActions() {
		return state.getActions();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getResultDisplay()
	 */
	@Override
	public ResultDisplay getResultDisplay() {
		return state.getResultDisplay();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getSelectedOperators()
	 */
	@Override
	public List<Operator> getSelectedOperators() {
		return state.getSelectedOperators();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getFirstSelectedOperator()
	 */
	@Override
	public Operator getFirstSelectedOperator() {
		return state.getFirstSelectedOperator();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#addProcessEditor(com.rapidminer.gui.processeditor.ProcessEditor)
	 */
	@Override
	public void addProcessEditor(ProcessEditor p) {
		state.addProcessEditor(p);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#selectOperator(com.rapidminer.operator.Operator)
	 */
	@Override
	public void selectOperator(Operator currentlySelected) {
		state.selectOperator(currentlySelected);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#selectOperators(java.util.List)
	 */
	@Override
	public void selectOperators(List<Operator> currentlySelected) {
		state.selectOperators(currentlySelected);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getDockingDesktop()
	 */
	@Override
	public DockingDesktop getDockingDesktop() {
		return state.getDockingDesktop();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getPerspectives()
	 */
	@Override
	public Perspectives getPerspectives() {
		return state.getPerspectives();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#handleBrokenProxessXML(com.rapidminer.ProcessLocation, java.lang.String, java.lang.Exception)
	 */
	@Override
	public void handleBrokenProxessXML(ProcessLocation location, String xml,
			Exception e) {
		state.handleBrokenProxessXML(location, xml, e);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getOperatorDocViewer()
	 */
	@Override
	public OperatorDocViewer getOperatorDocViewer() {
		return state.getOperatorDocViewer();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getProcessPanel()
	 */
	@Override
	public ProcessPanel getProcessPanel() {
		return state.getProcessPanel();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#registerDockable(com.vlsolutions.swing.docking.Dockable)
	 */
	@Override
	public void registerDockable(Dockable dockable) {
		state.registerDockable(dockable);
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getProcessContextEditor()
	 */
	@Override
	public ProcessContextProcessEditor getProcessContextEditor() {
		return state.getProcessContextEditor();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getXMLEditor()
	 */
	@Override
	public Component getXMLEditor() {
		return state.getXMLEditor();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getImportCsvFileAction()
	 */
	@Override
	public Action getImportCsvFileAction() {
		return state.getImportCsvFileAction();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getImportExcelFileAction()
	 */
	@Override
	public Action getImportExcelFileAction() {
		return state.getImportExcelFileAction();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getImportAccessFileAction()
	 */
	@Override
	public Action getImportAccessFileAction() {
		return state.getImportAccessFileAction();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getImportDatabaseTableAction()
	 */
	@Override
	public Action getImportDatabaseTableAction() {
		return state.getImportDatabaseTableAction();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getValidateAutomaticallyAction()
	 */
	@Override
	public ToggleAction getValidateAutomaticallyAction() {
		return state.getValidateAutomaticallyAction();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getRewireRecursively()
	 */
	@Override
	public Action getRewireRecursively() {
		return state.getRewireRecursively();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getRunAction()
	 */
	@Override
	public RunAction getRunAction() {
		return state.getRunAction();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getToggleExpertModeAction()
	 */
	@Override
	public ToggleAction getToggleExpertModeAction() {
		return state.getToggleExpertModeAction();
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.ProcessEndHandler#processEnded(com.rapidminer.Process, com.rapidminer.operator.IOContainer)
	 */
	@Override
	public void processEnded(Process process, IOContainer results) {
		state.processEnded(process, results);
	}

    // / LISTENERS


    /* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getToolsMenu()
	 */

    /* (non-Javadoc)
	 * @see com.rapidminer.gui.MainUIState#getProcessMenu()
	 */
}
