/*
 * 
 */
package com.rapidminer.gui;

import java.awt.Component;
import java.util.List;

import javax.swing.Action;
import javax.swing.JFrame;

import com.rapidminer.ProcessLocation;
import com.rapidminer.gui.actions.Actions;
import com.rapidminer.gui.actions.RunAction;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.operatortree.OperatorTree;
import com.rapidminer.gui.processeditor.NewOperatorEditor;
import com.rapidminer.gui.processeditor.ProcessContextProcessEditor;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.processeditor.results.ResultDisplay;
import com.rapidminer.gui.properties.OperatorPropertyPanel;
import com.rapidminer.gui.tools.LoggingViewer;
import com.rapidminer.gui.tools.StatusBar;
import com.rapidminer.operator.Operator;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockingDesktop;

/**
 * The interface providing the functionality of the UI for RapidMiner.
 * 
 * @author Gábor Bakos
 */
public interface MainUIState extends MenusUI, TutorialState, ProcessState {

	public void setExpertMode(boolean expert);

	public OperatorPropertyPanel getPropertyPanel();

	public LoggingViewer getMessageViewer();

	public NewOperatorEditor getNewOperatorEditor();

	public OperatorTree getOperatorTree();

	public Actions getActions();

	public ResultDisplay getResultDisplay();

	// /** Updates the list of recently used files. */
	// public void updateRecentFileList();

	/*
	 * public void windowOpened(WindowEvent e);
	 * 
	 * public void windowClosing(WindowEvent e);
	 * 
	 * public void windowClosed(WindowEvent e);
	 * 
	 * public void windowIconified(WindowEvent e);
	 * 
	 * public void windowDeiconified(WindowEvent e);
	 * 
	 * public void windowActivated(WindowEvent e);
	 * 
	 * public void windowDeactivated(WindowEvent e);
	 */

	public List<Operator> getSelectedOperators();

	public Operator getFirstSelectedOperator();

	public void addProcessEditor(ProcessEditor p);

	public void selectOperator(Operator currentlySelected);

	public void selectOperators(List<Operator> currentlySelected);

	public DockingDesktop getDockingDesktop();

	public Perspectives getPerspectives();

	public void handleBrokenProxessXML(ProcessLocation location, String xml,
			Exception e);

	public OperatorDocViewer getOperatorDocViewer();

	public ProcessPanel getProcessPanel();

	public void registerDockable(Dockable dockable);

	public ProcessContextProcessEditor getProcessContextEditor();

	public Component getXMLEditor();

	/**
	 * Returns the status bar of the application.
	 * 
	 * @return status bar
	 */
	public StatusBar getStatusBar();

	/**
	 * @return Saved?
	 */
	boolean close();

	/**
	 * @param relaunch
	 */
	void exit(boolean relaunch);

	JFrame getWindow();

	Action getImportCsvFileAction();

	Action getImportExcelFileAction();

	Action getImportAccessFileAction();

	Action getImportDatabaseTableAction();

	ToggleAction getValidateAutomaticallyAction();

	Action getRewireRecursively();

	RunAction getRunAction();

	ToggleAction getToggleExpertModeAction();
}