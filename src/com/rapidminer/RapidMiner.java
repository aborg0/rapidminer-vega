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
package com.rapidminer;

import java.awt.Frame;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.rapid_i.Launcher;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.SplashScreen;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypePassword;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.XMLSerialization;
import com.rapidminer.tools.cipher.CipherTools;
import com.rapidminer.tools.cipher.KeyGenerationException;
import com.rapidminer.tools.cipher.KeyGeneratorTool;
import com.rapidminer.tools.jdbc.DatabaseService;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;
import com.rapidminer.tools.plugin.Plugin;
import com.rapidminer.tools.usagestats.UsageStatistics;

/**
 * Main program. Entry point for command line programm, GUI and wrappers. Please note 
 * that applications which use RapidMiner as a data mining library will have to invoke one of the 
 * init methods provided by this class before applying processes or operators.
 * Several init methods exist and choosing the correct one with optimal parameters
 * might drastically reduce runtime and / or initialization time.
 * 
 * @author Ingo Mierswa
 */
public class RapidMiner {

	public static final String SYSTEM_ENCODING_NAME = "SYSTEM";

	public static enum ExitMode {
		NORMAL,
		ERROR,
		RELAUNCH
	}

	/** Indicates how RapidMiner is being executed. */
	public enum ExecutionMode {    	
		/** It is unknown how RM was invoked. */
		UNKNOWN(true, false, false),
		/** RM is executed using {@link RapidMinerCommandLine#main(String[])}. */
		COMMAND_LINE(true, true, false),
		/** RM is executed using {@link RapidMinerGUI#main(String[])}. */
		UI(false, true, true),
		/** RM is running inside an application server. */
		APPSERVER(true, false, false),
		/** RM is running as an applet inside a browser. */
		APPLET(false, true, true),
		/** RM is embedded into another program. */
		EMBEDDED_WITH_UI(false, true, false),    	
		/** RM is embedded into another program. */
		EMBEDDED_WITHOUT_UI(true, true, false),    	
		/** RM is running within Java Web Start. */
		WEBSTART(false, true, true);

		private final boolean isHeadless;
		private final boolean canAccessFilesystem;
		private final boolean hasMainFrame;
		private ExecutionMode(boolean isHeadless, boolean canAccessFilesystem, boolean hasMainFrame) {
			this.isHeadless = isHeadless;
			this.canAccessFilesystem = canAccessFilesystem; 
			this.hasMainFrame = hasMainFrame;
		}    	
		public boolean isHeadless() {
			return isHeadless;
		}
		public boolean canAccessFilesystem() {
			return canAccessFilesystem;
		}
		public boolean hasMainFrame() {
			return hasMainFrame;
		}
	}

	private static ExecutionMode executionMode = ExecutionMode.UNKNOWN;
	
	// ---  GENERAL PROPERTIES  ---
	
	/** The name of the property indicating the version of RapidMiner (read only). */
	public static final String PROPERTY_RAPIDMINER_VERSION = "rapidminer.version";

	/** Enables special features for developers: Validate process action, operator doc editor, etc. */
	public static final String PROPERTY_DEVELOPER_MODE = "rapidminer.developermode";
	
	/** The name of the property indicating the path to a additional operator description XML file(s). 
	 * If more than one, then the files have to be separated using the File.pathSeparator character.*/
	public static final String PROPERTY_RAPIDMINER_OPERATORS_ADDITIONAL = "rapidminer.operators.additional";

	/** The name of the property indicating the path to additional ioobjects description XML file(s). If more 
	 * than one, then the files have to be separated using the File.pathSeparator character.*/
	public static final String PROPERTY_RAPIDMINER_OBJECTS_ADDITIONAL = "rapidminer.objects.additional";

	/** The name of the property indicating the path to an RC file (settings). */
	public static final String PROPERTY_RAPIDMINER_RC_FILE = "rapidminer.rcfile";

	/** The name of the property indicating the path to the global logging file. */
	public static final String PROPERTY_RAPIDMINER_GLOBAL_LOG_FILE = "rapidminer.global.logging.file";

	/** The name of the property indicating the path to the global logging file. */
	public static final String PROPERTY_RAPIDMINER_GLOBAL_LOG_VERBOSITY = "rapidminer.global.logging.verbosity";


	// ---  INIT PROPERTIES  ---

	/** A file path to an operator description XML file. */
	public static final String PROPERTY_RAPIDMINER_INIT_OPERATORS = "rapidminer.init.operators";
	
	public static final String PROPERTY_RAPIDMINER_INIT_LOCALE_LANGUAGE = "rapidminer.init.locale.language";
	public static final String PROPERTY_RAPIDMINER_INIT_LOCALE_COUNTRY  = "rapidminer.init.locale.country";
	public static final String PROPERTY_RAPIDMINER_INIT_LOCALE_VARIANT  = "rapidminer.init.locale.variant";
	

//	/** A file path to the directory containing the JDBC drivers (usually the lib/jdbc directory of RapidMiner). */
//	public static final String PROPERTY_RAPIDMINER_INIT_JDBC_LIB_LOCATION = "rapidminer.init.jdbc.location";
//
//	/** Boolean parameter indicating if the drivers located in the lib directory of RapidMiner should be initialized. */
//	public static final String PROPERTY_RAPIDMINER_INIT_JDBC_LIB = "rapidminer.init.jdbc.lib";
//
//	/** Boolean parameter indicating if the drivers located somewhere in the classpath should be initialized. */
//	public static final String PROPERTY_RAPIDMINER_INIT_JDBC_CLASSPATH = "rapidminer.init.jdbc.classpath";

	/** Boolean parameter indicating if the plugins should be initialized at all. */
	public static final String PROPERTY_RAPIDMINER_INIT_PLUGINS = "rapidminer.init.plugins";

	/** A file path to the directory containing the plugin Jar files. */
	public static final String PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION = "rapidminer.init.plugins.location";


	// ---  OTHER PROPERTIES  ---

	/** The property name for &quot;The number of fraction digits of formatted numbers.&quot; */
	public static final String PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS = "rapidminer.general.fractiondigits.numbers";

	/** The property name for &quot;The number of fraction digits of formatted percent values.&quot; */
	public static final String PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_PERCENT = "rapidminer.general.fractiondigits.percent";

	/** The name of the property indicating the maximum number of attributes stored for shortened meta data transformation. */
	public static final String PROPERTY_RAPIDMINER_GENERAL_MAX_META_DATA_ATTRIBUTES = "rapidminer.general.md_attributes_limit";

	/** The name of the property indicating the maximum number of nominal values to store for meta data transformation. */
	public static final String PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES = "rapidminer.general.md_nominal_values_limit";
	
	/** The property name for &quot;Path to external Java editor. %f is replaced by filename and %l by the linenumber.&quot; */
	public static final String PROPERTY_RAPIDMINER_TOOLS_EDITOR = "rapidminer.tools.editor";

	/** The property specifying the method to send mails. Either SMTP or sendmail. */
	public static final String PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD = "rapidminer.tools.mail.method";
	public static final String[] PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES = { "sendmail", "SMTP" };
	public static final int PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SENDMAIL = 0;
	public static final int PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP     = 1;

	/** Property specifying the email address to which mails are sent if no email address is specified in the {@link ProcessRootOperator}. */
	public static final String PROPERTY_RAPIDMINER_TOOLS_MAIL_DEFAULT_RECIPIENT = "rapidminer.tools.mail.default_recipient";

	/** The default value of the minimum time a process must run such that it sends a notification mail upon completion. */
	public static final String PROPERTY_RAPIDMINER_TOOLS_MAIL_DEFAULT_PROCESS_DURATION_FOR_MAIL = "rapidminer.tools.mail.process_duration_for_mail";

	/** The property name for &quot;Path to sendmail. Used for email notifications.&quot; */
	public static final String PROPERTY_RAPIDMINER_TOOLS_SENDMAIL_COMMAND = "rapidminer.tools.sendmail.command";

	/** The property name for &quot;The smtp host. Used for email notifications.&quot; */
	public static final String PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST = "rapidminer.tools.smtp.host";

	/** The property name for &quot;The smtp port. Used for email notifications.&quot; */
	public static final String PROPERTY_RAPIDMINER_TOOLS_SMTP_PORT = "rapidminer.tools.smtp.port";

	/** The property name for the &quot;SMTP user. Used for email notifications.&quot; */
	public static final String PROPERTY_RAPIDMINER_TOOLS_SMTP_USER = "rapidminer.tools.smtp.user";

	/** The property name for the &quot;SMTP pssword (is necessary). Used for email notifications.&quot; */
	public static final String PROPERTY_RAPIDMINER_TOOLS_SMTP_PASSWD = "rapidminer.tools.smtp.passwd";

	/** If set to true, the query builders and database assistants and query_builders show only standard
	 *  tables (no views and system tables). */
	public static final String PROPERTY_RAPIDMINER_TOOLS_DB_ONLY_STANDARD_TABLES = "rapidminer.tools.db.assist.show_only_standard_tables";

	/** The property name for &quot;Use unix special characters for logfile highlighting (requires new RapidMiner instance).&quot; */
	public static final String PROPERTY_RAPIDMINER_GENERAL_LOGFILE_FORMAT = "rapidminer.general.logfile.format";

	/** The property name for &quot;Indicates if RapidMiner should be used in debug mode (print exception stacks and shows more technical error messages)&quot; */
	public static final String PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE = "rapidminer.general.debugmode";

	/** The name of the property indicating the default encoding for files. */
	public static final String PROPERTY_RAPIDMINER_GENERAL_DEFAULT_ENCODING = "rapidminer.general.encoding";

	/** The name of the property indicating the preferred globally used time zone. */
	public static final String PROPERTY_RAPIDMINER_GENERAL_TIME_ZONE = "rapidminer.general.timezone";


	/**
	 * A set of some non-gui and operator related system properties (starting with "rapidminer."). Properties
	 * can be registered using {@link RapidMiner#registerRapidMinerProperty(ParameterType)}.
	 */
	private static final java.util.Set<ParameterType> PROPERTY_TYPES = new java.util.TreeSet<ParameterType>();


	public static final String PROCESS_FILE_EXTENSION = "rmp";

	static {
		System.setProperty(PROPERTY_RAPIDMINER_VERSION, RapidMiner.getLongVersion());
		registerRapidMinerProperty(new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS, "The number of fraction digits of formatted numbers.", 0, Integer.MAX_VALUE, 3));
		registerRapidMinerProperty(new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_PERCENT, "The number of fraction digits of formatted percent values.", 0, Integer.MAX_VALUE, 2));
		registerRapidMinerProperty(new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES, "The number of nominal values to use for meta data transformation, 0 for unlimited. (Changing this value requires a cache refresh of the meta data for the current process, e.g. by changing the 'location' parameter of a 'Retrieve' operator.)", 0, Integer.MAX_VALUE, 100));
		registerRapidMinerProperty(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_EDITOR, "Path to external Java editor. %f is replaced by filename and %l by the linenumber.", true));
		registerRapidMinerProperty(new ParameterTypeCategory(PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD, "Method to send outgoing mails. Either SMTP or sendmail.", PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES, 0));
		registerRapidMinerProperty(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_MAIL_DEFAULT_RECIPIENT, "Default recipient for outgoing mails.", true));
		registerRapidMinerProperty(new ParameterTypeInt(PROPERTY_RAPIDMINER_TOOLS_MAIL_DEFAULT_PROCESS_DURATION_FOR_MAIL, "Default process duration time necessary to send notification emails (in minutes).", 0, Integer.MAX_VALUE, 30));

		registerRapidMinerProperty(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_SENDMAIL_COMMAND, "Path to sendmail. Used for email notifications.", true));
		registerRapidMinerProperty(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST, "SMTP host. Used for email notifications.", true));
		registerRapidMinerProperty(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_SMTP_PORT, "SMTP port, defaults to 25. Used for email notifications.", true));
		registerRapidMinerProperty(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_SMTP_USER, "SMTP user name. Used for email notifications.", true));
		registerRapidMinerProperty(new ParameterTypePassword(PROPERTY_RAPIDMINER_TOOLS_SMTP_PASSWD, "SMTP password, if required. Used for email notifications."));
		registerRapidMinerProperty(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GENERAL_LOGFILE_FORMAT, "Use unix special characters for logfile highlighting (requires new RapidMiner instance).", false));
		registerRapidMinerProperty(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE, "Indicates if RapidMiner should be used in debug mode (print exception stacks and shows more technical error messages)", false));
		registerRapidMinerProperty(new ParameterTypeString(PROPERTY_RAPIDMINER_GENERAL_DEFAULT_ENCODING, "The default encoding used for file operations (default: 'SYSTEM' uses the underlying system encoding, 'UTF-8' or 'ISO-8859-1' are other common options)", SYSTEM_ENCODING_NAME));
		registerRapidMinerProperty(new ParameterTypeCategory(PROPERTY_RAPIDMINER_GENERAL_TIME_ZONE, "The default time zone used for displaying date and time information (default: 'SYSTEM' uses the underlying system encoding, 'UCT', 'GMT' or 'CET' are other common options)", Tools.getAllTimeZones(), Tools.SYSTEM_TIME_ZONE));

		registerRapidMinerProperty(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_TOOLS_DB_ONLY_STANDARD_TABLES, "If checked, assistants and query builders will only show standard database tables, hiding system tables, views, etc.", true));
		
		registerRapidMinerProperty(new ParameterTypeBoolean(CapabilityProvider.PROPERTY_RAPIDMINER_GENERAL_CAPABILITIES_WARN, "Indicates if only a warning should be made if learning capabilities are not fulfilled (instead of breaking the process).", false));
		
		// INIT
//		registerRapidMinerProperty(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_INIT_JDBC_LIB, "Load JDBC drivers from lib dir?", true));
//		registerRapidMinerProperty(new ParameterTypeFile(PROPERTY_RAPIDMINER_INIT_JDBC_LIB_LOCATION, "Directory to scan for JDBC drivers.", null, true));		
//		registerRapidMinerProperty(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_INIT_JDBC_CLASSPATH, "Scan classpath for JDBC drivers (very time consuming)?", false));
		
		registerRapidMinerProperty(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_INIT_PLUGINS, "Initialize pluigins?", true));		
		registerRapidMinerProperty(new ParameterTypeFile(PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION, "Directory to scan for plugin jars.", null, true));
		
		registerRapidMinerProperty(new ParameterTypeBoolean("http.proxySet", "Determines whether a proxy is used for HTTP connections.", false));
		registerRapidMinerProperty(new ParameterTypeString("http.proxyHost", "The proxy host to use for HTTP.", true));
		registerRapidMinerProperty(new ParameterTypeInt("http.proxyPort", "The proxy port to use for HTTP.", 0, 65535, true));
		registerRapidMinerProperty(new ParameterTypeBoolean("https.proxySet", "Determines whether a proxy is used for HTTPS connections.", true));
		registerRapidMinerProperty(new ParameterTypeString("https.proxyHost", "The proxy host to use for HTTPS.", true));
		registerRapidMinerProperty(new ParameterTypeInt("https.proxyPort", "The proxy port to use for HTTPS.", 0, 65535, true));
		registerRapidMinerProperty(new ParameterTypeString("http.nonProxyHosts", "List of regular expressions determining hosts not to be connected directly.", true));
		
		registerRapidMinerProperty(new ParameterTypeBoolean("ftp.proxySet", "Determines whether a proxy is used for FTPconnections.", false));
		registerRapidMinerProperty(new ParameterTypeString("ftp.proxyHost", "The proxy host to use for FTP.", true));
		registerRapidMinerProperty(new ParameterTypeInt("ftp.proxyPort", "The proxy port to use for FTP.", 0, 65535, true));
		registerRapidMinerProperty(new ParameterTypeString("ftp.nonProxyHosts", "List of regular expressions determining hosts not to be connected directly.", true));
		
		registerRapidMinerProperty(new ParameterTypeString("socksProxyHost", "The proxy host to use for SOCKS.", true));
		registerRapidMinerProperty(new ParameterTypeInt("socksProxyPort", "The proxy port to use for SOCKS.", 0, 65535, true));
	}

	private static InputHandler inputHandler = new ConsoleInputHandler();

	private static SplashScreen splashScreen;

	private static final List<Runnable> shutdownHooks = new LinkedList<Runnable>();
	
	public static String getShortVersion() {
		return Version.getShortVersion();
	}

	public static String getLongVersion() {
		return Version.getLongVersion();
	}

	/**
	 * @deprecated Use {@link #readProcessFile(File)} instead
	 */
	@Deprecated
	public static Process readExperimentFile(File experimentfile) throws XMLException, IOException, InstantiationException, IllegalAccessException {
		return readProcessFile(experimentfile);
	}

	public static Process readProcessFile(File processFile) throws XMLException, IOException, InstantiationException, IllegalAccessException {
		return readProcessFile(processFile, null);
	}

	public static Process readProcessFile(File processFile, ProgressListener progressListener) throws XMLException, IOException, InstantiationException, IllegalAccessException {		
		try {
			LogService.getRoot().fine("Reading process file '" + processFile + "'.");
			if (!processFile.exists() || !processFile.canRead()) {
				LogService.getRoot().severe("Cannot read process definition file '" + processFile + "'!");
			}
			return new Process(processFile, progressListener);
		} catch (XMLException e) {
			throw new XMLException(processFile.getName() + ":" + e.getMessage(), e);
		}
	}

	/**
	 * Initializes RapidMiner.
	 * During initialization, the following system properties are evaluated. All can be
	 * specified in one of the RapidMiner configuration files, by using 
	 * {@link System#setProperty(String, String)}, or by passing a
	 * <code>-Dkey=value</code> to the Java executable.
	 *  
	 * <ul>
	 * <li>rapidminer.init.operators (file path)</li>
	 * <li>rapidminer.init.plugins (true or false)</li>
	 * <li>rapidminer.init.plugins.location (directory path)</li>
	 * <li>rapidminer.init.weka (true or false)</li>
	 * <li>rapidminer.init.jdbc.lib (true or false)</li>
	 * <li>rapidminer.init.jdbc.lib.location (directory path)</li>
	 * <li>rapidminer.init.jdbc.classpath (true or false)</li>
	 * </ul>
	 */
	public static void init() {		
		// set default locale to US
		String localeLanguage = System.getProperty(PROPERTY_RAPIDMINER_INIT_LOCALE_LANGUAGE);
		String localeCountry  = System.getProperty(PROPERTY_RAPIDMINER_INIT_LOCALE_COUNTRY);
		String localeVariant  = System.getProperty(PROPERTY_RAPIDMINER_INIT_LOCALE_VARIANT);
		if (localeLanguage != null) {
			Locale locale;
			if ((localeVariant != null) && (localeCountry != null)) {
				locale = new Locale(localeLanguage, localeCountry, localeVariant);
			} else if (localeCountry != null) {
				locale = new Locale(localeLanguage, localeCountry);
			} else {
				locale = new Locale(localeLanguage);
			}
			Locale.setDefault(locale);
			LogService.getRoot().config("Set default locale to "+locale);
		} else {			
			Locale.setDefault(Locale.US);
			LogService.getRoot().config("Locale not specified explicitly. Set default locale to US.");
		}

		RapidMiner.splashMessage("init_i18n");
		I18N.getErrorBundle();

		// ensure rapidminer.home is set
		RapidMiner.splashMessage("rm_home");
		ParameterService.ensureRapidMinerHomeSet();

		RapidMiner.splashMessage("init_parameter_service");
		ParameterService.init();

		RapidMiner.splashMessage("register_plugins");
		Plugin.initAll();
		Plugin.initPluginSplashTexts(RapidMiner.splashScreen);
		RapidMiner.showSplashInfos();

		//RapidMiner.splashMessage("init_setup");

		//LogService.getRoot().config("Default encoding is " + Tools.getDefaultEncoding()+".");
	
		RapidMiner.splashMessage("init_ops");
		OperatorService.init();
		
		UsageStatistics.getInstance(); // initializes as a side effect
		
		RapidMiner.splashMessage("xml_transformer");
		XMLImporter.init();

		RapidMiner.splashMessage("load_jdbc_drivers");
		DatabaseService.init();
		DatabaseConnectionService.init();

		RapidMiner.splashMessage("init_repository");
		RepositoryManager.init();

		RapidMiner.splashMessage("xml_serialization");
		XMLSerialization.init(Plugin.getMajorClassLoader());

		RapidMiner.splashMessage("xml_alias");
		OperatorService.defineXMLAliasPairs();		


		// generate encryption key if necessary
		if (!CipherTools.isKeyAvailable()) {
			RapidMiner.splashMessage("gen_key");
			try {
				KeyGeneratorTool.createAndStoreKey();
			} catch (KeyGenerationException e) {
				LogService.getRoot().log(Level.WARNING, "Cannot generate encryption key: " + e.getMessage(), e);
			}
		}

		// initialize renderers
		RapidMiner.splashMessage("init_renderers");
		RendererService.init();
	}

	private static void showSplashInfos() {
		if (getSplashScreen() != null)
			getSplashScreen().setInfosVisible(true);
	}

	public static SplashScreen showSplash() {
		URL url = Tools.getResource("rapidminer_logo.png");
		Image logo = null;
		try {
			if (url != null) {
				logo = ImageIO.read(url);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return showSplash(logo);
	}

	public static SplashScreen showSplash(Image productLogo) {
		RapidMiner.splashScreen = new SplashScreen(getShortVersion(), productLogo);
		RapidMiner.splashScreen.showSplashScreen();
		return RapidMiner.splashScreen;
	}

	public static void hideSplash() {
		RapidMiner.splashScreen.dispose();
	}

	/** Displays the message with 18n key gui.splash.messageKey. */
	public static void splashMessage(String messageKey) {
		if (RapidMiner.splashScreen != null) {
			RapidMiner.splashScreen.setMessage(I18N.getMessage(I18N.getGUIBundle(), "gui.splash."+messageKey));
		} else {
			LogService.getRoot().config(I18N.getMessage(I18N.getGUIBundle(), "gui.splash."+messageKey));
		}
	}

	/** Displays the formatted message with 18n key gui.splash.messageKey. */
	public static void splashMessage(String messageKey, Object ... args) {
		if (RapidMiner.splashScreen != null) {
			RapidMiner.splashScreen.setMessage(I18N.getMessage(I18N.getGUIBundle(), "gui.splash."+messageKey, args));
		}
	}


	public static SplashScreen getSplashScreen() {
		return RapidMiner.splashScreen;
	}

	public static Frame getSplashScreenFrame() {
		if (RapidMiner.splashScreen != null)
			return RapidMiner.splashScreen.getSplashScreenFrame();
		else
			return null;
	}

	public static void setInputHandler(InputHandler inputHandler) {
		RapidMiner.inputHandler = inputHandler;
	}

	public static InputHandler getInputHandler() {
		return inputHandler;
	}

	/** Returns a set of {@link ParameterType}s for the RapidMiner system properties. 
	 * @deprecated Use {@link #getRapidMinerProperties()} instead*/
	@Deprecated
	public static java.util.Set<ParameterType> getYaleProperties() {
		return getRapidMinerProperties();
	}

	/** Returns a set of {@link ParameterType}s for the RapidMiner system properties. */
	public static java.util.Set<ParameterType> getRapidMinerProperties() {
		return PROPERTY_TYPES;
	}

	/**
	 * @deprecated Use {@link #registerRapidMinerProperty(ParameterType)} instead
	 */
	@Deprecated
	public static void registerYaleProperty(ParameterType type) {
		registerRapidMinerProperty(type);
	}

	public static void registerRapidMinerProperty(ParameterType type) {
		PROPERTY_TYPES.add(type);
	}
	
	public synchronized static void addShutdownHook(Runnable runnable) {
		shutdownHooks.add(runnable);
	}

	public synchronized static void quit(ExitMode exitMode) {
		for (Runnable hook : shutdownHooks) {
			try {
				hook.run();
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, "Error executing shutdown hook: " + e.getMessage(), e);
			}
		}
		try {
			Runtime.getRuntime().runFinalization();
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Error during finalization: " + e.getMessage(), e);
		}
		switch (exitMode) {
		case NORMAL:
			System.exit(0);
			break;
		case ERROR:
			System.exit(1);
			break;
		case RELAUNCH:
			Launcher.relaunch();
			break;
		}			
	}

	public static ExecutionMode getExecutionMode() {
		return executionMode;
	}

	public static void setExecutionMode(ExecutionMode executionMode) {
		RapidMiner.executionMode = executionMode;
	}
	
	public static void setLocale(String language, String country, String variant) {
		System.setProperty(PROPERTY_RAPIDMINER_INIT_LOCALE_LANGUAGE, language);
		System.setProperty(PROPERTY_RAPIDMINER_INIT_LOCALE_COUNTRY, country);
		System.setProperty(PROPERTY_RAPIDMINER_INIT_LOCALE_VARIANT, variant);
		
	}
}
