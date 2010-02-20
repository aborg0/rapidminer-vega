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
package com.rapidminer.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import com.rapid_i.Launcher;
import com.rapidminer.RapidMiner;
import com.rapidminer.Version;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.parameter.ParameterType;

/**
 * This class loads the yalerc property files and provides methods to access
 * them. It also provides methods to create files relative to the RapidMiner home
 * directory. As the {@link #getProperty(String)} method throws an exception if
 * the parameter is not set, the <code>System.getProperty(String)</code>
 * methods should be used if this is not desired.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class ParameterService {

	/** Property specifying the root directory of the RapidMiner project sources. */
	public static final String PROPERTY_RAPIDMINER_SRC_ROOT = "rapidminer.src.root";
	private static final String ENVIRONMENT_RAPIDMINER_CONFIG_DIR = "RAPIDMINER_CONFIG_DIR";
	private static final String PROPERTY_RAPIDMINER_CONFIG_DIR    = "rapidminer.config.dir";

	/**
	 * Tries to find the rapidminer.home directory if the property is not set and sets
	 * it.
	 */
	public static void ensureRapidMinerHomeSet() {
		Launcher.ensureRapidMinerHomeSet();
//		String home = System.getProperty(Launcher.PROPERTY_RAPIDMINER_HOME);
//		if (home != null) {
//			LogService.getRoot().config(Launcher.PROPERTY_RAPIDMINER_HOME + " is '" + home + "'.");
//		} else {
//			LogService.getRoot().config("Property " + Launcher.PROPERTY_RAPIDMINER_HOME+ " is not set. Guessing.");
//			String classpath = System.getProperty("java.class.path");
//			String pathComponents[] = classpath.split(File.pathSeparator);
//			boolean found = false;
//			for (int i = 0; i < pathComponents.length; i++) {
//				String path = pathComponents[i].trim();
//				if (path.endsWith("rapidminer.jar")) {
//					File jar = new File(path).getAbsoluteFile();
//					String message = "Trying parent directory of '" + jar + "'...";
//					File dir = jar.getParentFile();
//					if (dir != null) {
//						dir = dir.getParentFile();
//						if (dir != null) {
//							message += "gotcha!";
//							found = true;
//							System.setProperty(Launcher.PROPERTY_RAPIDMINER_HOME, dir.getAbsolutePath());
//						} else {
//							message += "failed";
//						}
//					} else {
//						message += "failed";
//					}
//					LogService.getRoot().log(Level.CONFIG, message);
//				}
//			}
//
//			if (!found) {
//				String message = "Trying base directory of classes (build) '";
//				URL url = ParameterService.class.getClassLoader().getResource(".");
//				if (url != null) {
//					try {
//						File dir = new File(new URI(url.toString()));
//						if (dir.exists()) {
//							dir = dir.getParentFile();
//							message += dir + "'...";
//							if (dir != null) {
//								message += "gotcha!";
//								try {
//									System.setProperty(Launcher.PROPERTY_RAPIDMINER_HOME, dir.getCanonicalPath());
//								} catch (IOException e) {
//									System.setProperty(Launcher.PROPERTY_RAPIDMINER_HOME, dir.getAbsolutePath());
//								}
//							} else {
//								message += "failed";
//							}
//						} else {
//							message += "failed";
//						}
//					} catch (URISyntaxException e) {
//						message += "failed";    
//					}
//				} else {
//					message += "failed";
//				}
//				LogService.getRoot().log(Level.CONFIG, message);
//			}
//		}
//		getProperty(Launcher.PROPERTY_RAPIDMINER_HOME); // throws exception if necessary
	}


	/** Invokes {@link #init(InputStream, boolean)} with a null stream meaning that
	 *  the core operators.xml is loaded and with addWekaOperators = true.
	 *  Registers the operators from the stream and reads the rc file. */
	public static void init() {
		init(null);
	}

	/** Reads user and system wide configuration files (as long as this is allowed
	 *  by the {@link ExecutionMode}). */
	public static void init(InputStream operatorsXMLStream) {
		loadAllRCFiles();
	}

	public static void copyMainUserConfigFile(VersionNumber oldVersion, VersionNumber newVersion) {
		Properties oldProperties = readPropertyFile(getVersionedUserConfigFile(oldVersion, "rapidminerrc" + "." + System.getProperty("os.name")));
		writeProperties(oldProperties, getMainUserConfigFile());
	}

	private static Properties readPropertyFile(File file) {
		if (file.exists()) {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "Cannot read main user properties: " + e.getMessage(), e);
				return new Properties();
			}
			try {
				return readPropertyFile(in);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						LogService.getRoot().log(Level.WARNING, "Cannot close connection to user properties: " + e.getMessage(), e);
					}
				}
			}
		} else {
			return new Properties();
		}
	}
	
	/** Reads a property file, possibly transforming properties as required by the {@link ParameterType}s. */
	private static Properties readPropertyFile(InputStream in) {
		Properties properties = new Properties();
		try {
			properties.load(in);
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Cannot read main user properties: " + e.getMessage(), e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING, "Cannot close connection to user properties: " + e.getMessage(), e);
				}
			}
		}
		for (Map.Entry<Object,Object> entry : properties.entrySet()) {				
			String typeKey = (String)entry.getKey();
			String typeValue = (String)entry.getValue();							
			if (typeValue != null) {					
				for (ParameterType type : RapidMiner.getRapidMinerProperties()) {					
					if (type.getKey().equals(typeKey)) {
						properties.put(typeKey, type.transformNewValue(typeValue));						
						break;
					}
				}				
			}
		}
		return properties;
	}

	public static void writeProperties(Properties properties, File file) {
		if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
			LogService.getRoot().config("Ignoring request to save properties file in execution mode "+RapidMiner.getExecutionMode()+".");
			return;
		}
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(getMainUserConfigFile()));
			for (Map.Entry<Object,Object> entry : properties.entrySet()) {				
				String typeKey = (String)entry.getKey();
				String typeValue = (String)entry.getValue();							
				if (typeValue != null) {					
					for (ParameterType type : RapidMiner.getRapidMinerProperties()) {					
						if (type.getKey().equals(typeKey)) {
							typeValue = type.toString(typeValue);
							break;
						}
					}	
					//System.setProperty(typeKey, typeValue);
					out.println(typeKey + " = " + typeValue);
				}
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Cannot write user properties: " + e.getMessage(), e);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	public static void writePropertyIntoMainUserConfigFile(String key, String value) {
		if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
			LogService.getRoot().config("Ignoring request to save properties file in execution mode "+RapidMiner.getExecutionMode()+".");
			return;
		}
		LogService.getRoot().config("Saving property "+key+"="+value);
		// read old configuration
		Properties userProperties = readPropertyFile(getMainUserConfigFile());

		// set new property
		userProperties.setProperty(key, value);
		System.setProperty(key, value);

		// write complete configuration back into the file
		writeProperties(userProperties, getMainUserConfigFile());
	}

	/** Returns the main user configuration file containing the version number and the OS. */
	public static File getMainUserConfigFile() {
		return ParameterService.getUserConfigFile("rapidminerrc" + "." + System.getProperty("os.name"));	
	}

	/** Returns the configuration file in the user dir .RapidMiner5 and automatically adds 
	 *  the current version number if it is a rc file. */
	public static File getUserConfigFile(String name) {
		return getVersionedUserConfigFile(new VersionNumber(Version.getLongVersion()), name);
	}

	public static File getVersionedUserConfigFile(VersionNumber versionNumber, String name) {
		String configName = name;
		if (configName.startsWith("rapidminerrc")) {
			if (versionNumber != null)
				configName = versionNumber.toString().replaceAll("\\.", "_") + "_" + configName;
		}
		return new File(getUserRapidMinerDir(), configName);		
	}

	public static File getUserRapidMinerDir() {
		File homeDir = new File(System.getProperty("user.home"));
		File userHomeDir = new File(homeDir, ".RapidMiner5");
		if (!userHomeDir.exists()) {
			LogService.getRoot().config("Creating directory '" + userHomeDir + "'.");
			boolean result = userHomeDir.mkdir();
			if (!result)
				LogService.getRoot().warning("Unable to create user home rapidminer directory " + userHomeDir);
		}
		return userHomeDir;
	}
	
	private static void loadAllRCFiles() {
		InputStream in = ParameterService.class.getResourceAsStream("/"+Tools.RESOURCE_PREFIX+"rapidminerrc");
		if (in != null) {
			LogService.getRoot().config("Reading configuration resource "+Tools.RESOURCE_PREFIX+"rapidminerrc.");
			Properties props = readPropertyFile(in);
			System.getProperties().putAll(props);
		} else {
			LogService.getRoot().warning("Resource "+Tools.RESOURCE_PREFIX+"rapidminerrc is missing.");
		}
		if (RapidMiner.getExecutionMode().canAccessFilesystem()) {
			File globalRC = getGlobalConfigFile("rapidminerrc");
			if (globalRC != null) {
				loadRCFile(globalRC.getAbsolutePath());
			}
			loadAllRCFiles(getUserConfigFile("rapidminerrc").getAbsolutePath());
			loadAllRCFiles(new File(new File(System.getProperty("user.dir")), "rapidminerrc").getAbsolutePath());
			String localRC = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_RC_FILE);
			if (localRC != null) {
				loadRCFile(localRC);
			} else {
				LogService.getRoot().config("Property "+RapidMiner.PROPERTY_RAPIDMINER_RC_FILE+" not specified...skipped");
			}
		} else {
			LogService.getRoot().config("Execution mode "+RapidMiner.getExecutionMode()+" does not permit file access. Ignoring all rcfiles.");
		}
	}

	private static void loadAllRCFiles(String rcFileName) {
		loadRCFile(rcFileName);
		loadRCFile(rcFileName + "." + System.getProperty("os.name"));
	}

	private static void loadRCFile(String rcFileName) {
		if (rcFileName == null)
			return;
		File rcFile = new File(rcFileName);
		if (!rcFile.exists()) {
			LogService.getRoot().config("Trying rcfile '" + rcFile + "'...skipped");
			return;
		}
		Properties props = readPropertyFile(rcFile);
		System.getProperties().putAll(props);
		LogService.getRoot().config("Read rcfile '" + rcFile + "'.");
	}

	// -------------------- parameters --------------------

//	/**
//	 * Returns a system property and throws a runtime exception if the property
//	 * is not set.
//	 */
//	private static String getProperty(String key) {
//		String property = System.getProperty(key);
//		if (property == null) {
//			throw new RuntimeException("Property '" + key + "' not set!");
//		}
//		return property;
//	}

	public static File getRapidMinerHome() throws IOException {		
		String property = System.getProperty(Launcher.PROPERTY_RAPIDMINER_HOME);
		if (property == null) {
			throw new IOException("Property "+Launcher.PROPERTY_RAPIDMINER_HOME+" is not set");
		}
		return new File(property);
	}

	private static File getLibraryFile(String name) throws IOException {
		File home = getRapidMinerHome();
		return new File(home, "lib" + File.separator + name);
	}

	public static File getPluginDir() throws IOException {
		return getLibraryFile("plugins");
	}
	
	// -------------------- tools --------------------

	/**
	 * Returns true if value is "true", "yes", "y" or "on". Returns false if
	 * value is "false", "no", "n" or "off". Otherwise returns <tt>deflt</tt>.
	 */
	public static boolean booleanValue(String value, boolean deflt) {
		if (value == null)
			return deflt;
		if (value.equals("true"))
			return true;
		else if (value.equals("yes"))
			return true;
		else if (value.equals("y"))
			return true;
		else if (value.equals("on"))
			return true;
		else if (value.equals("false"))
			return false;
		else if (value.equals("no"))
			return false;
		else if (value.equals("n"))
			return false;
		else if (value.equals("off"))
			return false;

		return deflt;
	}


	public static File getSourceRoot() {
		String srcName = System.getProperty(PROPERTY_RAPIDMINER_SRC_ROOT);
		if (srcName == null) {
			LogService.getRoot().warning("Property " + PROPERTY_RAPIDMINER_SRC_ROOT+ " not set.");
			return null;
		} else {
			return new File(srcName);
		}
	}
	
	public static File getSourceFile(String name) {
		File root = getSourceRoot();
		if (root == null) {
			return null;
		} else {
			return new File(new File(root, "src"), name);
		}
	}
	
	public static File getSourceResourceFile(String name) {		
		File root = getSourceRoot();
		if (root == null) {
			return null;
		} else {
			return new File(new File(root, "resources"), name);
		}
	}

	public static File getGlobalConfigFile(String fileName) {
		File dir = getGlobalConfigDir();
		if (dir != null) {
			File result = new File(dir, fileName);
			if (result.exists()) {
				if (result.canRead()) {
					return result;
				} else {
					LogService.getRoot().warning("Config file "+result.getAbsolutePath()+" is not readable.");
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	private static File getGlobalConfigDir() {
		String configDir = System.getProperty(PROPERTY_RAPIDMINER_CONFIG_DIR);
		if (configDir == null) {
			configDir = System.getenv(ENVIRONMENT_RAPIDMINER_CONFIG_DIR);
		}
		if (configDir != null) {
			File dir = new File(configDir);
			if (dir.exists()) {
				if (dir.canRead()) {
					return dir;
				} else {
					LogService.getRoot().warning("Directory "+dir.getAbsolutePath()+" specified by environment variable "+ENVIRONMENT_RAPIDMINER_CONFIG_DIR+" is not readable.");
					return null;
				}
			} else {
				LogService.getRoot().warning("Directory "+dir.getAbsolutePath()+" specified by environment variable "+ENVIRONMENT_RAPIDMINER_CONFIG_DIR+" does not exist.");
				return null;
			}
		} else {
			LogService.getRoot().config("Neither system property '"+PROPERTY_RAPIDMINER_CONFIG_DIR+"' nor environment variable '"+ENVIRONMENT_RAPIDMINER_CONFIG_DIR+"' not set. Ignored.");
			return null;
		}
	}
}
