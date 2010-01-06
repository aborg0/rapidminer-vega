/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
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

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import com.rapid_i.deployment.update.client.UpdateManager;
import com.rapidminer.FileProcessLocation;
import com.rapidminer.NoOpUserError;
import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RapidMiner;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.actions.ImportProcessAction;
import com.rapidminer.gui.dialog.ResultHistory;
import com.rapidminer.gui.docking.RapidDockableContainerFactory;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.fc.BookmarkIO;
import com.rapidminer.gui.look.ui.RapidDockingUISettings;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;
import com.rapidminer.gui.viewer.MetaDataViewerTableModel;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;
import com.rapidminer.tools.plugin.Plugin;
import com.rapidminer.tools.usagestats.UsageStatistics;
import com.rapidminer.tools.usagestats.UsageStatsTransmissionDialog;
import com.vlsolutions.swing.docking.DockableContainerFactory;
import com.vlsolutions.swing.docking.ui.DockingUISettings;


/**
 * The main class if RapidMiner is started in GUI mode. This class keeps a reference to the
 * {@link MainFrame} and some other GUI relevant information and methods.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class RapidMinerGUI extends RapidMiner {

	public static final String PROPERTY_GEOMETRY_X                      = "rapidminer.gui.geometry.x";

	public static final String PROPERTY_GEOMETRY_Y                      = "rapidminer.gui.geometry.y";

	public static final String PROPERTY_GEOMETRY_WIDTH                  = "rapidminer.gui.geometry.width";

	public static final String PROPERTY_GEOMETRY_HEIGHT                 = "rapidminer.gui.geometry.height";

	public static final String PROPERTY_GEOMETRY_DIVIDER_MAIN           = "rapidminer.gui.geometry.divider.main";

	public static final String PROPERTY_GEOMETRY_DIVIDER_EDITOR         = "rapidminer.gui.geometry.divider.editor";;

	public static final String PROPERTY_GEOMETRY_DIVIDER_LOGGING        = "rapidminer.gui.geometry.divider.logging";

	public static final String PROPERTY_GEOMETRY_DIVIDER_GROUPSELECTION = "rapidminer.gui.geometry.divider.groupselection";

	public static final String PROPERTY_EXPERT_MODE                     = "rapidminer.gui.expertmode";

	// --- Properties ---

	public static final String PROPERTY_RAPIDMINER_GUI_UPDATE_CHECK  = "rapidminer.update.check";

	public static final String PROPERTY_RAPIDMINER_GUI_MAX_STATISTICS_ROWS = "rapidminer.gui.max_statistics_rows";

	public static final String PROPERTY_RAPIDMINER_GUI_MAX_SORTABLE_ROWS = "rapidminer.gui.max_sortable_rows";

	public static final String PROPERTY_RAPIDMINER_GUI_MAX_DISPLAYED_VALUES = "rapidminer.gui.max_displayed_values";

	public static final String PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID = "rapidminer.gui.snap_to_grid";

	public static final String PROPERTY_AUTOWIRE_INPUT                    = "rapidminer.gui.autowire_input";

	public static final String PROPERTY_AUTOWIRE_OUTPUT                     = "rapidminer.gui.autowire_output";

	public static final String PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS = "rapidminer.gui.resolve_relative_repository_locations";
	
	public static final String PROPERTY_CLOSE_RESULTS_BEFORE_RUN         = "rapidminer.gui.close_results_before_run";

	public static final String PROPERTY_TRANSFER_USAGESTATS = "rapidminer.gui.transfer_usagestats";

	public static final String[] PROPERTY_TRANSFER_USAGESTATS_ANSWERS = { "ask", "always", "never" };

	public static final String PROPERTY_ADD_BREAKPOINT_RESULTS_TO_HISTORY = "rapidminer.gui.add_breakpoint_results_to_history";
	
	static {
		RapidMiner.registerRapidMinerProperty(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GUI_UPDATE_CHECK, "Check for new RapidMiner versions at start up time?", true)); 
		RapidMiner.registerRapidMinerProperty(new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_MAX_STATISTICS_ROWS, "Indicates the maximum number of rows for the automatic calculation of statistics and other time intensive data viewing actions.", 1, Integer.MAX_VALUE, 100000));
		RapidMiner.registerRapidMinerProperty(new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_MAX_SORTABLE_ROWS, "Indicates the maximum number of rows for sortable tables.", 1, Integer.MAX_VALUE, 100000));
		RapidMiner.registerRapidMinerProperty(new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_MAX_DISPLAYED_VALUES, "Indicates the maximum number of different values which will for example be displayed in the meta data view.", 1, Integer.MAX_VALUE, MetaDataViewerTableModel.DEFAULT_MAX_DISPLAYED_VALUES));
		RapidMiner.registerRapidMinerProperty(new ParameterTypeBoolean(PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS, "If checked, the repository browser dialog will resolve repository locations relative to the current process by default. Can be disabled within the dialog.", true));
		RapidMiner.registerRapidMinerProperty(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID, "If checked, operators snap to the grid.", true));
		RapidMiner.registerRapidMinerProperty(new ParameterTypeBoolean(PROPERTY_AUTOWIRE_INPUT, "If checked, operator's inputs are wired automatically when added. Can be checked also in the \"Operators\" tree.", true));
		RapidMiner.registerRapidMinerProperty(new ParameterTypeBoolean(PROPERTY_AUTOWIRE_OUTPUT, "If checked, operator's outputs are wired automatically when added. Can be checked also in the \"Operators\" tree.", true));
		RapidMiner.registerRapidMinerProperty(new ParameterTypeCategory(PROPERTY_CLOSE_RESULTS_BEFORE_RUN, "Close active result tabs when new process starts?", DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.ASK));
		
		RapidMiner.registerRapidMinerProperty(new ParameterTypeCategory(RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS, "Allow RapidMiner to transfer RapidMiner operator usage statistics?", RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS_ANSWERS, UsageStatsTransmissionDialog.ASK));
		RapidMiner.registerRapidMinerProperty(new ParameterTypeBoolean(RapidMinerGUI.PROPERTY_ADD_BREAKPOINT_RESULTS_TO_HISTORY, "Should results produced at breakpoints be added to the result history?", false));
		
		// UPDATE
		RapidMiner.registerRapidMinerProperty(new ParameterTypeBoolean(com.rapid_i.deployment.update.client.UpdateManager.PARAMETER_UPDATE_INCREMENTALLY, "Download (small) patches rather than complete installation archives?", true));
		RapidMiner.registerRapidMinerProperty(new ParameterTypeString(com.rapid_i.deployment.update.client.UpdateManager.PARAMETER_UPDATE_URL, "URL of the RapidMiner update server.", com.rapid_i.deployment.update.client.UpdateManager.UPDATESERVICE_URL));
		RapidMiner.registerRapidMinerProperty(new ParameterTypeBoolean(com.rapid_i.deployment.update.client.UpdateManager.PARAMETER_INSTALL_TO_HOME, "If checked, all upgrades will be installed to the users home directory. Otherwise, administrator privileges are required.", false));	
	}

	private static final int NUMBER_OF_RECENT_FILES = 8;

	private static MainFrame mainFrame;

	private static LinkedList<ProcessLocation> recentFiles = new LinkedList<ProcessLocation>();

	private static ResultHistory resultHistory = new ResultHistory();

	/**
	 * This thread listens for System shutdown and cleans up after shutdown.
	 * This included saving the recent file list and other GUI properties.
	 */
	private static class ShutdownHook extends Thread {
		@Override
		public void run() {
			LogService.getRoot().info("Running shutdown sequence.");
			RapidMinerGUI.saveRecentFileList();
			RapidMinerGUI.saveGUIProperties();
			UsageStatistics.getInstance().save();
			RepositoryManager.shutdown();
		}
	}

	//private static UpdateManager updateManager = new CommunityUpdateManager();

	public void run(File file) throws Exception {
		// check if resources were copied
		URL logoURL = Tools.getResource("rapidminer_logo.png");
		if (logoURL == null) {
			LogService.getRoot().severe("Cannot find resources. Probably the ant target 'copy-resources' must be performed!");
			RapidMiner.quit(RapidMiner.ExitMode.ERROR);
		}

		// Initialize Docking UI -- important must be done as early as possible!
		DockingUISettings.setInstance(new RapidDockingUISettings());
		DockableContainerFactory.setFactory(new RapidDockableContainerFactory());
		
		RapidMiner.showSplash();

		// set locale fix to US
		Locale.setDefault(Locale.US);
		JComponent.setDefaultLocale(Locale.US);

		RapidMiner.splashMessage("basic");
		RapidMiner.init();

		// check if this version is started for the first time
		RapidMiner.splashMessage("workspace");
		performInitialSettings();

		RapidMiner.splashMessage("plaf");
		setupToolTipManager();
		setupGUI();

		RapidMiner.splashMessage("history");
		loadRecentFileList();

		RapidMiner.splashMessage("icons");
		SwingTools.loadIcons();

		RepositoryManager.getInstance().createRepositoryIfNoneIsDefined();
		
		RapidMiner.splashMessage("create_frame");
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				setMainFrame(new MainFrame());				
			}
		});

		RapidMiner.splashMessage("gui_properties");
		loadGUIProperties(mainFrame);

		RapidMiner.splashMessage("plugin_gui");
		Plugin.initPluginGuis(mainFrame);

		RapidMiner.splashMessage("show_frame");

		mainFrame.setVisible(true);

		UsageStatsTransmissionDialog.init();

		RapidMiner.splashMessage("checks");
		Plugin.initFinalChecks();

		RapidMiner.splashMessage("ready");

		RapidMiner.hideSplash();

		// file from command line or Welcome Dialog
		if (file != null) {
			ImportProcessAction.open(file);
		}

		// check for updates
		Plugin.initPluginUpdateManager();
		UpdateManager.checkForUpdates();
	}

	private void setupToolTipManager() {
		// setup tool tip text manager
		ToolTipManager manager = ToolTipManager.sharedInstance();
		manager.setDismissDelay(25000); // original: 4000
		manager.setInitialDelay(1500);   // original: 750
		manager.setReshowDelay(50);    // original: 500
	}

	/** This default implementation only setup the tool tip durations. Subclasses might
	 *  override this method. */
	protected void setupGUI() throws NoOpUserError {
		System.setProperty(BookmarkIO.PROPERTY_BOOKMARKS_DIR, ParameterService.getUserRapidMinerDir().getAbsolutePath());
		System.setProperty(BookmarkIO.PROPERTY_BOOKMARKS_FILE, ".bookmarks");
		System.setProperty(DatabaseConnectionService.PROPERTY_CONNECTIONS_DIR, ParameterService.getUserRapidMinerDir().getAbsolutePath());
		System.setProperty(DatabaseConnectionService.PROPERTY_CONNECTIONS_FILE, ".connections");
		try {
			UIManager.setLookAndFeel(new RapidLookAndFeel());
			//OperatorService.reloadIcons();
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Cannot setup modern look and feel, using default.", e);
		} 			
	}

	public static void setMainFrame(MainFrame mf) {
		mainFrame = mf;
	}

	public static MainFrame getMainFrame() {
		return mainFrame;
	}

	private void performInitialSettings() {
		boolean firstStart = false;
		VersionNumber lastVersionNumber = null;
		VersionNumber currentVersionNumber = new VersionNumber(getLongVersion());

		File lastVersionFile = new File(ParameterService.getUserRapidMinerDir(), "lastversion");
		if (!lastVersionFile.exists()) {
			firstStart = true;
		} else {
			String versionString = null;
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(lastVersionFile));
				versionString = in.readLine();
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "Cannot read global version file of last used version.", e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						LogService.getRoot().log(Level.WARNING, "Cannnot close stream to file " + lastVersionFile, e);
					}
				}
			}

			if (versionString != null) {
				lastVersionNumber = new VersionNumber(versionString);			
				if (currentVersionNumber.compareTo(lastVersionNumber) > 0) {
					firstStart = true;
				}
			} else {
				firstStart = true;
			}
		}

		// init this version (workspace etc.)
		if (firstStart) {
			performFirstInitialization(lastVersionNumber, currentVersionNumber);
		}

		// write version file
		writeLastVersion(lastVersionFile);	
	}

	private void performFirstInitialization(VersionNumber lastVersion, VersionNumber currentVersion) {
		if (currentVersion != null)
			LogService.getRoot().info("Performing upgrade" + (lastVersion != null ? " from version " + lastVersion : "") + " to version " + currentVersion);

		// copy old settings to new version file
		ParameterService.copyMainUserConfigFile(lastVersion, currentVersion);
	}

	private void writeLastVersion(File versionFile) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(versionFile));
			out.println(getLongVersion());
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Cannot write current version into property file.", e);
		} finally {
			if (out != null)
				out.close();
		}
	}

	public static void useProcessFile(Process process) {
		ProcessLocation location = process.getProcessLocation();
		if (location != null) {
			while (recentFiles.contains(location)) {
				recentFiles.remove(location);
			}
			recentFiles.addFirst(location);
			while (recentFiles.size() > NUMBER_OF_RECENT_FILES)
				recentFiles.removeLast();
		}		
		saveRecentFileList();
	}

	public static ResultHistory getResultHistory() {
		return resultHistory;
	}

	public static List<ProcessLocation> getRecentFiles() {
		return recentFiles;
	}

	private static void loadRecentFileList() {
		File file = ParameterService.getUserConfigFile("history");
		if (!file.exists())
			return;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			recentFiles.clear();
			String line = null;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("file ")) {
					recentFiles.add(new FileProcessLocation(new File(line.substring(5))));	
				} else if (line.startsWith("repository ")) {
					recentFiles.add(new RepositoryProcessLocation(new RepositoryLocation(line.substring(11))));
				} else {
					LogService.getRoot().log(Level.WARNING, "Unparseable line in history file: "+line);		
				}				
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Cannot read history file", e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING, "Cannot read history file", e);
				}
			}
		}
	}

	private static void saveRecentFileList() {
		File file = ParameterService.getUserConfigFile("history");
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(file));
			for (ProcessLocation loc : recentFiles) {
				out.println(loc.toHistoryFileString());
			}
		} catch (IOException e) {
			SwingTools.showSimpleErrorMessage("cannot_write_history_file", e);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private static void saveGUIProperties() {
		Properties properties = new Properties();
		MainFrame mainFrame = getMainFrame();
		if (mainFrame != null) {
			properties.setProperty(PROPERTY_GEOMETRY_X, "" + (int) mainFrame.getLocation().getX());
			properties.setProperty(PROPERTY_GEOMETRY_Y, "" + (int) mainFrame.getLocation().getY());
			properties.setProperty(PROPERTY_GEOMETRY_WIDTH, "" + mainFrame.getWidth());
			properties.setProperty(PROPERTY_GEOMETRY_HEIGHT, "" + mainFrame.getHeight());
			//properties.setProperty(PROPERTY_GEOMETRY_DIVIDER_MAIN, "" + mainFrame.getMainDividerLocation());
			//properties.setProperty(PROPERTY_GEOMETRY_DIVIDER_EDITOR, "" + mainFrame.getEditorDividerLocation());
			//properties.setProperty(PROPERTY_GEOMETRY_DIVIDER_LOGGING, "" + mainFrame.getLoggingDividerLocation());
			//properties.setProperty(PROPERTY_GEOMETRY_DIVIDER_GROUPSELECTION, "" + mainFrame.getGroupSelectionDividerLocation());
			properties.setProperty(PROPERTY_EXPERT_MODE, "" + mainFrame.getPropertyPanel().isExpertMode());
			File file = ParameterService.getUserConfigFile("gui.properties");
			OutputStream out = null;
			try {
				out = new FileOutputStream(file);
				properties.store(out, "RapidMiner GUI properties");
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "Cannot write GUI properties: " + e.getMessage(), e);
			} finally {
				try {
					if (out != null)
						out.close();
				} catch (IOException e) { }
			}
			mainFrame.getResultDisplay().clearAll();
			mainFrame.getPerspectives().saveAll();
		}
	}

	private static void loadGUIProperties(MainFrame mainFrame) {
		Properties properties = new Properties();
		File file = ParameterService.getUserConfigFile("gui.properties");
		if (file.exists()) {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
				properties.load(in);
			} catch (IOException e) {
				setDefaultGUIProperties();
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException e) {
					throw new Error(e); // should not occur
				}
			}
			try {
				mainFrame.setLocation(Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_X)), Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_Y)));
				mainFrame.setSize(new Dimension(Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_WIDTH)), Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_HEIGHT))));
				mainFrame.setExpertMode(Boolean.valueOf(properties.getProperty(PROPERTY_EXPERT_MODE)).booleanValue());
			} catch (NumberFormatException e) {
				setDefaultGUIProperties();
			}
		} else {
			setDefaultGUIProperties();
		}
		mainFrame.getPerspectives().loadAll();		
	}

	/** This method sets some default GUI properties. This method can be invoked if the properties
	 *  file was not found or produced any error messages (which might happen after version changes).
	 */
	private static void setDefaultGUIProperties() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setLocation((int)(0.05d * screenSize.getWidth()), (int)(0.05d * screenSize.getHeight()));
		mainFrame.setSize((int)(0.9d * screenSize.getWidth()), (int)(0.9d * screenSize.getHeight()));
		//mainFrame.setDividerLocations((int)(0.6d * screenSize.getHeight()), (int)(0.2d * screenSize.getWidth()), (int)(0.75d * screenSize.getWidth()), (int)(0.4d * screenSize.getWidth()));
		mainFrame.setExpertMode(false);
	}

	public static void main(String[] args) throws Exception {
		System.setSecurityManager(null);
		setExecutionMode(ExecutionMode.UI);
		RapidMiner.addShutdownHook(new ShutdownHook());
	
		File file = null;
		if (args.length > 0) {
			if (args.length != 1) {
				System.out.println("java " + RapidMinerGUI.class.getName() + " [processfile]");
				return;
			}
			file = new File(args[0]);
			if (!file.exists()) {
				System.err.println("File '" + args[0] + "' not found.");
				return;
			}
			if (!file.canRead()) {
				System.err.println("Cannot read file '" + args[0] + "'.");
				return;
			}
		}
		RapidMiner.setInputHandler(new GUIInputHandler());
		new RapidMinerGUI().run(file);
	}
}
