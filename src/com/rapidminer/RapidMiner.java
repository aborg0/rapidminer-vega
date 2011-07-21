/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.rapid_i.Launcher;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.SplashScreen;
import com.rapidminer.gui.tools.VersionNumber;
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
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.GlobalAuthenticator;
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
        UNKNOWN(true, false, false, true),
        /** RM is executed using {@link RapidMinerCommandLine#main(String[])}. */
        COMMAND_LINE(true, true, false, true),
        /** RM is executed using {@link RapidMinerGUI#main(String[])}. */
        UI(false, true, true, true),
        /** RM is running inside an application server. */
        APPSERVER(true, false, false, false),
        /** RM is running as an applet inside a browser. */
        APPLET(false, true, true, false),
        /** RM is embedded into another program. */
        EMBEDDED_WITH_UI(false, true, false, false),
        /** RM is embedded into another program. */
        EMBEDDED_WITHOUT_UI(true, true, false, false),
        /** RM is embedded into an applet. */
        EMBEDDED_AS_APPLET(false, false, false, false),
        /** RM is running within Java Web Start. */
        WEBSTART(false, true, true, true),
        /** We are executing unit tests. */
        TEST(true, true, false, true);

        private final boolean isHeadless;
        private final boolean canAccessFilesystem;
        private final boolean hasMainFrame;
        private final boolean loadManagedExtensions;
        private ExecutionMode(boolean isHeadless, boolean canAccessFilesystem, boolean hasMainFrame, boolean loadManagedExtensions) {
            this.isHeadless = isHeadless;
            this.canAccessFilesystem = canAccessFilesystem;
            this.hasMainFrame = hasMainFrame;
            this.loadManagedExtensions = loadManagedExtensions;
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
        public boolean isLoadingManagedExtensions() {
            return loadManagedExtensions;
        }
    }

    private static ExecutionMode executionMode = ExecutionMode.UNKNOWN;
    private static VersionNumber version = new RapidMinerVersion();

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


    // Webstart properties
    public static final String PROPERTY_HOME_REPOSITORY_URL = "rapidminer.homerepository.url";
    public static final String PROPERTY_HOME_REPOSITORY_USER = "rapidminer.homerepository.user";
    public static final String PROPERTY_HOME_REPOSITORY_PASSWORD= "rapidminer.homerepository.password";



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

    /** The name of the property defining how many lines are read for guessing values types for input operations without defined value type. */
    public static final String PROPERTY_RAPIDMINER_GENERAL_MAX_TEST_ROWS = "rapidminer.general.max_rows_used_for_guessing";

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


    // ---  INIT PROPERTIES  ---

    public static final String PROPERTY_RAPIDMINER_HTTP_PROXY_SET = "http.proxySet";
    public static final String PROPERTY_RAPIDMINER_HTTP_PROXY_HOST = "http.proxyHost";
    public static final String PROPERTY_RAPIDMINER_HTTP_PROXY_PORT = "http.proxyPort";
    public static final String PROPERTY_RAPIDMINER_HTTP_PROXY_NON_PROXY_HOSTS = "http.nonProxyHosts";
    public static final String PROPERTY_RAPIDMINER_HTTP_PROXY_USERNAME = "http.proxyUsername";
    public static final String PROPERTY_RAPIDMINER_HTTP_PROXY_PASSWORD = "http.proxyPassword";

    public static final String PROPERTY_RAPIDMINER_HTTPS_PROXY_SET = "https.proxySet";
    public static final String PROPERTY_RAPIDMINER_HTTPS_PROXY_HOST = "https.proxyHost";
    public static final String PROPERTY_RAPIDMINER_HTTPS_PROXY_PORT = "https.proxyPort";
    public static final String PROPERTY_RAPIDMINER_HTTPS_PROXY_USERNAME = "https.proxyUsername";
    public static final String PROPERTY_RAPIDMINER_HTTPS_PROXY_PASSWORD = "https.proxyPassword";


    public static final String PROPERTY_RAPIDMINER_FTP_PROXY_SET = "ftp.proxySet";
    public static final String PROPERTY_RAPIDMINER_FTP_PROXY_HOST = "ftp.proxyHost";
    public static final String PROPERTY_RAPIDMINER_FTP_PROXY_PORT = "ftp.proxyPort";
    public static final String PROPERTY_RAPIDMINER_FTP_PROXY_USERNAME = "ftp.proxyUsername";
    public static final String PROPERTY_RAPIDMINER_FTP_PROXY_PASSWORD = "ftp.proxyPassword";
    public static final String PROPERTY_RAPIDMINER_FTP_PROXY_NON_PROXY_HOSTS = "ftp.nonProxyHosts";

    public static final String PROPERTY_RAPIDMINER_SOCKS_PROXY_HOST = "socksProxyHost";
    public static final String PROPERTY_RAPIDMINER_SOCKS_PROXY_PORT = "socksProxyPort";


    public static final String PROCESS_FILE_EXTENSION = "rmp";

    static {
        System.setProperty(PROPERTY_RAPIDMINER_VERSION, RapidMiner.getLongVersion());
        ParameterService.setParameterValue(PROPERTY_RAPIDMINER_VERSION, RapidMiner.getLongVersion());

        ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS, "The number of fraction digits of formatted numbers.", 0, Integer.MAX_VALUE, 3));
        ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_PERCENT, "The number of fraction digits of formatted percent values.", 0, Integer.MAX_VALUE, 2));
        ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES, "The number of nominal values to use for meta data transformation, 0 for unlimited. (Changing this value requires a cache refresh of the meta data for the current process, e.g. by changing the 'location' parameter of a 'Retrieve' operator.)", 0, Integer.MAX_VALUE, 100));
        ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_MAX_TEST_ROWS, "The number of lines read during input operations to guess the value type of certain columns if not specified. If set to 0, all rows will be used", 0, Integer.MAX_VALUE, 100));
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_EDITOR, "Path to external Java editor. %f is replaced by filename and %l by the linenumber.", true));
        ParameterService.registerParameter(new ParameterTypeCategory(PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD, "Method to send outgoing mails. Either SMTP or sendmail.", PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES, 0));
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_MAIL_DEFAULT_RECIPIENT, "Default recipient for outgoing mails.", true));
        ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_TOOLS_MAIL_DEFAULT_PROCESS_DURATION_FOR_MAIL, "Default process duration time necessary to send notification emails (in minutes).", 0, Integer.MAX_VALUE, 30));

        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_SENDMAIL_COMMAND, "Path to sendmail. Used for email notifications.", true));
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST, "SMTP host. Used for email notifications.", true));
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_SMTP_PORT, "SMTP port, defaults to 25. Used for email notifications.", true));
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_SMTP_USER, "SMTP user name. Used for email notifications.", true));
        ParameterService.registerParameter(new ParameterTypePassword(PROPERTY_RAPIDMINER_TOOLS_SMTP_PASSWD, "SMTP password, if required. Used for email notifications."));
        ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GENERAL_LOGFILE_FORMAT, "Use unix special characters for logfile highlighting (requires new RapidMiner instance).", false));
        ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE, "Indicates if RapidMiner should be used in debug mode (print exception stacks and shows more technical error messages)", false));
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_GENERAL_DEFAULT_ENCODING, "The default encoding used for file operations (default: 'SYSTEM' uses the underlying system encoding, 'UTF-8' or 'ISO-8859-1' are other common options)", SYSTEM_ENCODING_NAME));
        ParameterService.registerParameter(new ParameterTypeCategory(PROPERTY_RAPIDMINER_GENERAL_TIME_ZONE, "The default time zone used for displaying date and time information (default: 'SYSTEM' uses the underlying system encoding, 'UCT', 'GMT' or 'CET' are other common options)", Tools.getAllTimeZones(), Tools.SYSTEM_TIME_ZONE));

        ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_TOOLS_DB_ONLY_STANDARD_TABLES, "If checked, assistants and query builders will only show standard database tables, hiding system tables, views, etc.", true));

        ParameterService.registerParameter(new ParameterTypeBoolean(CapabilityProvider.PROPERTY_RAPIDMINER_GENERAL_CAPABILITIES_WARN, "Indicates if only a warning should be made if learning capabilities are not fulfilled (instead of breaking the process).", false));

        // INIT
        //		ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_INIT_JDBC_LIB, "Load JDBC drivers from lib dir?", true));
        //		ParameterService.registerParameter(new ParameterTypeFile(PROPERTY_RAPIDMINER_INIT_JDBC_LIB_LOCATION, "Directory to scan for JDBC drivers.", null, true));
        //		ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_INIT_JDBC_CLASSPATH, "Scan classpath for JDBC drivers (very time consuming)?", false));

        ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_INIT_PLUGINS, "Initialize pluigins?", true));
        ParameterService.registerParameter(new ParameterTypeFile(PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION, "Directory to scan for plugin jars.", null, true));

        ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_HTTP_PROXY_SET, "Determines whether a proxy is used for HTTP connections.", false), "system");
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_HTTP_PROXY_HOST, "The proxy host to use for HTTP.", true), "system");
        ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_HTTP_PROXY_PORT, "The proxy port to use for HTTP.", 0, 65535, true), "system");
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_HTTP_PROXY_USERNAME, "The user name for the http proxy server in cases where it needs authentication.", true), "system");
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_HTTP_PROXY_PASSWORD, "The password for the http proxy server in cases where it needs authentication.", true) , "system");

        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_HTTP_PROXY_NON_PROXY_HOSTS, "List of regular expressions separated by '|' determining hosts to be connected directly bypassing the proxy.", true), "system");

        ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_HTTPS_PROXY_SET, "Determines whether a proxy is used for HTTPS connections.", false), "system");
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_HTTPS_PROXY_HOST, "The proxy host to use for HTTPS.", true), "system");
        ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_HTTPS_PROXY_PORT, "The proxy port to use for HTTPS.", 0, 65535, true), "system");
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_HTTPS_PROXY_USERNAME, "The user name for the https proxy server in cases where it needs authentication.", true), "system");
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_HTTPS_PROXY_PASSWORD, "The password for the https proxy server in cases where it needs authentication.", true), "system");


        ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_FTP_PROXY_SET, "Determines whether a proxy is used for FTPconnections.", false), "system");
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_FTP_PROXY_HOST, "The proxy host to use for FTP.", true), "system");
        ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_FTP_PROXY_PORT, "The proxy port to use for FTP.", 0, 65535, true), "system");
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_FTP_PROXY_NON_PROXY_HOSTS, "List of regular expressions separated by '|' determining hosts to be connected directly bypassing the proxy.", true), "system");
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_FTP_PROXY_USERNAME, "The user name for the ftp proxy server in cases where it needs authentication.", true), "system");
        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_FTP_PROXY_PASSWORD, "The password for the ftp proxy server in cases where it needs authentication.", true), "system");


        ParameterService.registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_SOCKS_PROXY_HOST, "The proxy host to use for SOCKS.", true), "system");
        ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_SOCKS_PROXY_PORT, "The proxy port to use for SOCKS.", 0, 65535, true), "system");
    }

    private static InputHandler inputHandler = new ConsoleInputHandler();

    private static SplashScreen splashScreen;

    private static final List<Runnable> shutdownHooks = new LinkedList<Runnable>();

    public static String getShortVersion() {
        return version.getShortVersion();
    }

    public static String getLongVersion() {
        return version.getLongVersion();
    }

    public static VersionNumber getVersion() {
        return version;
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
            if (localeVariant != null && localeCountry != null) {
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
        // check if this version is started for the first time
        performInitialSettings();
        ParameterService.init();

        // initializing networking tools
        GlobalAuthenticator.init();

        // registering operators
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

        // initialize xml serialization
        RapidMiner.splashMessage("xml_serialization");
        XMLSerialization.init(Plugin.getMajorClassLoader());
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

    private static void performInitialSettings() {
        boolean firstStart = false;
        VersionNumber lastVersionNumber = null;
        VersionNumber currentVersionNumber = new VersionNumber(getLongVersion());

        File lastVersionFile = new File(FileSystemService.getUserRapidMinerDir(), "lastversion");
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

    private static void writeLastVersion(File versionFile) {
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

    private static void performFirstInitialization(VersionNumber lastVersion, VersionNumber currentVersion) {
        if (currentVersion != null)
            LogService.getRoot().info("Performing upgrade" + (lastVersion != null ? " from version " + lastVersion : "") + " to version " + currentVersion);

        // copy old settings to new version file
        ParameterService.copyMainUserConfigFile(lastVersion, currentVersion);
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

    /** Returns a set of {@link ParameterType}s for the RapidMiner system properties.
     * @deprecated Use {@link #getRapidMinerProperties()} instead*/
    @Deprecated
    public static Set<ParameterType> getYaleProperties() {
        return getRapidMinerProperties();
    }

    /**
     * Use {@link ParameterService#getDefinedParameterTypes()} instead.
     * Returns a set of {@link ParameterType}s for the RapidMiner system properties.
     * */
    @Deprecated
    public static Set<ParameterType> getRapidMinerProperties() {
        return ParameterService.getDefinedParameterTypes();
    }

    /**
     * @deprecated Use {@link #ParameterService.registerParameter(ParameterType)} instead
     */
    @Deprecated
    public static void registerYaleProperty(ParameterType type) {
        ParameterService.registerParameter(type);
    }

    /**
     * Please use {@link ParameterService#registerParameter(ParameterType)} instead.
     * 
     * This registers a property with the name of the given ParameterType. For convenience
     * the property is of this type, for offering the user a reasonable interface.
     */
    @Deprecated
    public static void registerRapidMinerProperty(ParameterType type) {
        ParameterService.registerParameter(type);
    }

    /**
     * This method is deprecated and remains only for compatiblity reasons.
     * Please refer to {@link ParameterService#getParameterValue(String)} instead.
     * 
     * This method will return the value of an registered RapidMiner Property or null
     * if no property is known with the given identifier.
     * @param property The identifier of the property
     * @return the String value of the property or null if property is unknown.
     */
    @Deprecated
    public static String getRapidMinerPropertyValue(String property) {
        return ParameterService.getParameterValue(property);
    }

    /**
     * This method will set the given property to the given value.
     * Please use {@link ParameterService#setParameterValue(String, String)} instead of this method.
     */
    @Deprecated
    public static void setRapidMinerPropertyValue(String property, String value) {
        ParameterService.setParameterValue(property, value);
    }
}
