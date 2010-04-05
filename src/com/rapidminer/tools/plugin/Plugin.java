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
package com.rapidminer.tools.plugin;

import java.awt.Frame;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.rapid_i.Launcher;
import com.rapid_i.deployment.update.client.ManagedExtension;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.flow.ProcessRenderer;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.templates.BuildingBlock;
import com.rapidminer.gui.tools.SplashScreen;
import com.rapidminer.gui.tools.dialogs.AboutBox;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ResourceSource;
import com.rapidminer.tools.Tools;

/**
 * <p>
 * The class for RapidMiner plugins. This class is used to encapsulate the .jar file which must be in the
 * <code>lib/plugins</code> subdirectory of RapidMiner. Provides methods for plugin checks, operator registering, and
 * getting information about the plugin.
 * </p>
 * <p>
 * Plugin dependencies must be defined in the form <br />
 * plugin_name1 (plugin_version1) # ... # plugin_nameM (plugin_versionM) < /br> of the manifest parameter
 * <code>Plugin-Dependencies</code>. You must define both the name and the version of the desired plugins and separate
 * them with &quot;#&quot;.
 * </p>
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class Plugin {

	/**
	 * The name for the manifest entry RapidMiner-Type which can be used to indicate that a jar file is a RapidMiner
	 * plugin.
	 */
	public static final String RAPIDMINER_TYPE = "RapidMiner-Type";

	/** The value for the manifest entry RapidMiner-Type which indicates that a jar file is a RapidMiner plugin. */
	public static final String RAPIDMINER_TYPE_PLUGIN = "RapidMiner_Extension";

	private static final ClassLoader MAJOR_CLASS_LOADER = new AllPluginsClassLoader();

	/**
	 * The jar archive of the plugin which must be placed in the <code>lib/plugins</code> subdirectory of RapidMiner.
	 */
	private JarFile archive;

	/** The file for this plugin. */
	private final File file;

	/** The class loader based on the plugin file. */
	private PluginClassLoader classLoader;

	/** The name of the plugin. */
	private String name;

	/** The version of the plugin. */
	private String version;

	/** The vendor of the plugin. */
	private String vendor;

	/** The url for this plugin (in WWW). */
	private String url;

	/** The RapidMiner version which is needed for this plugin. */
	private String necessaryRapidMinerVersion = "0";

	/** The plugins and their versions which are needed for this plugin. */
	private final List<Dependency> pluginDependencies = new LinkedList<Dependency>();
	
	private String extensionId;

	private String pluginInitClassName;

	private String pluginResourceObjects;

	private String pluginResourceOperators;

	private String pluginParseRules;

	private String pluginGroupDescriptions;

	private String pluginErrorDescriptions;

	private String pluginUserErrorDescriptions;

	private String pluginGUIDescriptions;

	private String prefix;

	private boolean disabled = false;

	/** The collection of all plugins. */
	private static List<Plugin> allPlugins = new LinkedList<Plugin>();

	/** Creates a new plugin based on the plugin .jar file. */
	public Plugin(File file) throws IOException {
		this.file = file;
		this.archive = new JarFile(this.file);
		URL url = new URL("file", null, this.file.getAbsolutePath());
		this.classLoader = new PluginClassLoader(new URL[] { url });
		Tools.addResourceSource(new ResourceSource(this.classLoader));
		getMetaData();
	}

	/** Returns the name of the plugin. */
	public String getName() {
		return name;
	}

	/** Returns the version of this plugin. */
	public String getVersion() {
		return version;
	}

	/** Returns the necessary RapidMiner version. */
	public String getNecessaryRapidMinerVersion() {
		return necessaryRapidMinerVersion;
	}

	/**
	 * Returns the class name of the plugin init class
	 */
	public String getPluginInitClassName() {
		return pluginInitClassName;
	}
	 
	public String getPluginParseRules() {
		return pluginParseRules;
	}

	public String getPluginGroupDescriptions() {
		return pluginGroupDescriptions;
	}

	public String getPluginErrorDescriptions() {
		return pluginErrorDescriptions;
	}

	public String getPluginUserErrorDescriptions() {
		return pluginUserErrorDescriptions;
	}

	public String getPluginGUIDescriptions() {
		return pluginGUIDescriptions;
	}
	/**
	 * Returns the resource identifier of the xml file specifying the operators
	 */
	public String getPluginResourceOperators() {
		return pluginResourceOperators;
	}

	/**
	 * Returns the resource identifier of the IO Object descriptions.
	 */
	public String getPluginResourceObjects() {
		return pluginResourceObjects;
	}

	/** Returns the plugin dependencies of this plugin. */
	public List getPluginDependencies() {
		return pluginDependencies;
	}

	/**
	 * Returns the class loader of this plugin. This class loader should be used in cases where Class.forName(...)
	 * should be used, e.g. for implementation finding in all classes (including the core and the plugins).
	 */
	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * Returns the class loader of this plugin. This class loader should be used in cases where Class.forName(...)
	 * should find a class explicitly defined in this plugin jar.
	 */
	public ClassLoader getOriginalClassLoader() {
		try {
			this.archive = new JarFile(this.file);
			URL url = new URL("file", null, this.file.getAbsolutePath());
			return new URLClassLoader(new URL[] { url }, Plugin.class.getClassLoader());
		} catch (IOException e) {
			return null;
		}
	}

	/** Checks the RapidMiner version and plugin dependencies. */
	private boolean checkDependencies(List plugins) {
		if (RapidMiner.getLongVersion().compareTo(necessaryRapidMinerVersion) < 0)
			return false;
		if (pluginDependencies.size() > 1) {
			throw new UnsupportedOperationException("Only one dependent plugin allowed!");
		}
		// other plugins
		Iterator i = pluginDependencies.iterator();
		while (i.hasNext()) {
			Dependency dependency = (Dependency) i.next();
			if (!dependency.isFulfilled(plugins))
				return false;
		}
		// all ok
		return true;
	}

	/** Collects all meta data of the plugin from the manifest file. */
	private void getMetaData() {
		try {
			java.util.jar.Attributes atts = archive.getManifest().getMainAttributes();
			name = getValue(atts, "Implementation-Title");
			
			if (name == null) {
				name = archive.getName();
			}
			version = getValue(atts, "Implementation-Version");
			if (version == null)
				version = "";

			url = getValue(atts, "Implementation-URL");
			vendor = getValue(atts, "Implementation-Vendor");

			prefix = getValue(atts, "Namespace");
			extensionId = getValue(atts, "Extension-ID");
			pluginInitClassName = getValue(atts, "Initialization-Class");
			pluginResourceObjects = getDescriptorResource("IOObject-Descriptor", false, false, atts);
			pluginResourceOperators = getDescriptorResource("Operator-Descriptor", false, true, atts);
			pluginParseRules = getDescriptorResource("ParseRule-Descriptor", false, false, atts);
			pluginGroupDescriptions = getDescriptorResource("Group-Descriptor", false, false, atts);
			
			pluginErrorDescriptions = getDescriptorResource("Error-Descriptor", false, true, atts);
			pluginUserErrorDescriptions = getDescriptorResource("UserError-Descriptor", false, true, atts);
			pluginGUIDescriptions = getDescriptorResource("GUI-Descriptor", false, true, atts);

			
			necessaryRapidMinerVersion = getValue(atts, "RapidMiner-Version");
			if (necessaryRapidMinerVersion == null) {
				necessaryRapidMinerVersion = "0";
			}
			String dependencies = getValue(atts, "Plugin-Dependencies");
			if (dependencies == null)
				dependencies = "";
			addDependencies(dependencies);

			RapidMiner.splashMessage("loading_plugin", name);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getValue(java.util.jar.Attributes atts, String key) {
		String result = atts.getValue(key);
		if (result == null) {
			return null;
		} else {
			result = result.trim();
			if (result.isEmpty()) {
				return null;
			} else {
				return result;
			}
		}
		
	}

	private String getDescriptorResource(String typeName, boolean mandatory, boolean isBundle, java.util.jar.Attributes atts) throws IOException {
		String value = getValue(atts, typeName);
		if (value == null) {
			if (mandatory) {
				throw new IOException("Manifest attribute '"+typeName+"' is not defined.");
			} else {
				return null;
			}
		} else {
			if (isBundle) {
				return toResourceBundleIdentifier(value);
			} else {
				return toResourceIdentifier(value);
			}
		}
	}
	
	private String toResourceBundleIdentifier(String value) {
		if (value.startsWith("/"))
			value = value.substring(1);
		if (value.endsWith(".properties")) {
			value = value.substring(0, value.length() - 11);
		}
		return value;
	}

	/**
	 * Removes leading slash if present.
	 */
	private String toResourceIdentifier(String value) {
		if (value.startsWith("/"))
			value = value.substring(1);
		return value;
	}
	
	/** Register plugin dependencies. */
	private void addDependencies(String dependencies) {		
		pluginDependencies.addAll(Dependency.parse(dependencies));
	}

	public void registerOperators() {
		if (disabled) {
			LogService.getRoot().warning("Plugin "+getName()+" disabled due to previous errors. Not registering operators.");
		}
		InputStream in = null;
		// trying normal plugins
		if (pluginResourceOperators != null) {
			URL operatorsURL = this.classLoader.getResource(pluginResourceOperators);
			if (operatorsURL == null) {
				LogService.getRoot().log(Level.WARNING, "Operator descriptor '"+pluginResourceOperators+"' does not exist in '" + archive.getName() + "'!");
				return;
			} else {
				// register operators
				try {
					in = operatorsURL.openStream();
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING, "Cannot read operator descriptor '"+operatorsURL+"' from '" + archive.getName() + "'!", e);
					return;
				}
			}
		} else if (pluginInitClassName != null) {			
			LogService.getRoot().info("No operator descriptor specified for plugin "+getName()+". Trying plugin initializtation class "+pluginInitClassName+".");
			// if no operators.xml found: Try via PluginInit method getOperatorStream()
			try {
				// important: here the combined class loader has to be used				
				Class<?> pluginInitator = Class.forName(pluginInitClassName, false, getClassLoader());
				Method registerOperatorMethod = pluginInitator.getMethod("getOperatorStream", new Class[] { ClassLoader.class });
				in = (InputStream) registerOperatorMethod.invoke(null, new Object[] { getClassLoader() });
			} catch (ClassNotFoundException e) {
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
		}
		if (in != null) {
			// add URLs of plugins this plugin depends on
			Iterator<Dependency> i = pluginDependencies.iterator();
			while (i.hasNext()) {
				String pluginName = i.next().getPluginExtensionId();
				Plugin other = getPluginByExtensionId(pluginName);

				// Using classLoader of dependent plugin as parent
				//TODO: Needs to cope with multiple dependencies: Solve Graph problem of dependencies
				this.classLoader = new PluginClassLoader(classLoader.getURLs(), other);
			}

			OperatorService.registerOperators(archive.getName(), in, this.classLoader, this);
		} else {
			LogService.getRoot().warning("No operator descriptor defined for: "+getName());
		}
	}
	
	/** Register all things delivered with this plugin. 
	 * @throws PluginException */
	public void registerDescriptions() throws PluginException {
		// registering settings for internationalization
		if (pluginErrorDescriptions != null)
			I18N.registerErrorBundle(ResourceBundle.getBundle(pluginErrorDescriptions, Locale.getDefault(), this.classLoader));
		if (pluginGUIDescriptions != null)
			I18N.registerGUIBundle(ResourceBundle.getBundle(pluginGUIDescriptions, Locale.getDefault(), this.classLoader));
		if (pluginUserErrorDescriptions != null)
			I18N.registerUserErrorMessagesBundle(ResourceBundle.getBundle(pluginUserErrorDescriptions, Locale.getDefault(), this.classLoader));

		// registering renderers
		if (pluginResourceObjects != null) {
			URL resource = this.classLoader.getResource(pluginResourceObjects);
			if (resource != null) {
				RendererService.init(name, resource, this.classLoader);				
			} else {
				throw new PluginException("Cannot find io object descriptor '"+pluginResourceObjects+"' for plugin "+getName()+".");
			}
		}
		
		// registering parse rules
		if (pluginParseRules != null) {
			URL resource = this.classLoader.getResource(pluginParseRules);
			if (resource != null) {
				XMLImporter.importParseRules(resource, this);
			} else {
				throw new PluginException("Cannot find parse rules '"+pluginParseRules+"' for plugin "+getName()+".");
			}
		}
		
		// registering colors
		if (pluginGroupDescriptions != null) {
			ProcessRenderer.registerAdditionalObjectColors(pluginGroupDescriptions, name, classLoader);
			ProcessRenderer.registerAdditionalGroupColors(pluginGroupDescriptions, name, classLoader);
		}
	}

	/**
	 * Returns a list of building blocks. If this plugin does not define any building blocks, an empty list will be
	 * returned.
	 */
	public List<BuildingBlock> getBuildingBlocks() {
		List<BuildingBlock> result = new LinkedList<BuildingBlock>();

		URL url = null;
		try {
			url = new URL("file", null, this.file.getAbsolutePath());
		} catch (MalformedURLException e1) {
			LogService.getRoot().log(Level.WARNING, "Cannot load plugin building blocks. Skipping...", e1);
		}
		if (url != null) {
			ClassLoader independentLoader = new PluginClassLoader(new URL[] { url });
			URL bbDefinition = independentLoader.getResource(Tools.RESOURCE_PREFIX + "buildingblocks.txt");
			if (bbDefinition != null) {
				BufferedReader in = null;
				try {
					in = new BufferedReader(new InputStreamReader(bbDefinition.openStream()));

					String line = null;
					while ((line = in.readLine()) != null) {
						URL bbURL = this.classLoader.getResource(Tools.RESOURCE_PREFIX + line);
						BufferedReader bbIn = null;
						try {
							bbIn = new BufferedReader(new InputStreamReader(bbURL.openStream()));
							result.add(new BuildingBlock(bbIn, BuildingBlock.PLUGIN_DEFINED));
						} catch (IOException e) {
							LogService.getRoot().log(Level.WARNING, "Cannot load plugin building blocks. Skipping...", e);
						} finally {
							if (bbIn != null) {
								bbIn.close();
							}
						}
					}
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING, "Cannot load plugin building blocks.", e);
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
							LogService.getRoot().log(Level.WARNING, "Cannot close stream to plugin building blocks.", e);
						}
					}
				}
			}
		}
		return result;
	}

	/** Creates the about box for this plugin. */
	public AboutBox createAboutBox(Frame owner) {
		String about = "";
		try {
			URL url = this.classLoader.getResource("META-INF/ABOUT.NFO");
			if (url != null)
				about = Tools.readTextFile(new InputStreamReader(url.openStream()));
		} catch (Exception e) {	
			LogService.getRoot().log(Level.WARNING, "Error reading ABOUT.NFO for plugin "+getName(), e);
		}
		Image productLogo = null;
		try {
			InputStream imageIn = this.classLoader.getResourceAsStream("META-INF/icon.png");
			productLogo = ImageIO.read(imageIn);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Error reading icon.png for plugin "+getName(), e);	
		}
		return new AboutBox(owner, name, version, "Vendor: " + ((vendor != null) ? vendor : "unknown"), url, about, true, productLogo);
	}

	/** Returns a list of plugins found in the plugins directory. */
	private static void findPlugins(File pluginDir, boolean showWarningForNonPluginJars) {
		if (!(pluginDir.exists() && pluginDir.isDirectory()))
			return;

		List<File> files = new LinkedList<File>();
		files.addAll(Arrays.asList(pluginDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		})));
		files.addAll(ManagedExtension.getActivePluginJars());

		allPlugins = new LinkedList<Plugin>();
		for (File file : files) {
			try {
				JarFile jarFile = new JarFile(file);
				Manifest manifest = jarFile.getManifest();
				Attributes attributes = manifest.getMainAttributes();
				if (RAPIDMINER_TYPE_PLUGIN.equals(attributes.getValue(RAPIDMINER_TYPE))) {
					allPlugins.add(new Plugin(file));
				} else {
					if (showWarningForNonPluginJars)
						LogService.getRoot().warning("The jar file '" + jarFile.getName() + "' does not contain an entry '" + RAPIDMINER_TYPE + "' in its manifest and will therefore not be loaded (if this file actually is a plugin updating the plugin file might help).");
				}
			} catch (Throwable e) {
				LogService.getRoot().log(Level.WARNING, "Cannot load plugin '" + file + "': " + e.getMessage(), e);
			}
		}
	}

	@Override
	public String toString() {
		return name + " " + version + " (" + archive.getName() + ") depending on " + pluginDependencies;
	}

	/**
	 * Finds all plugins in lib/plugins directory and initializes them.
	 */
	private static void registerAllPluginDescriptions(File pluginDir, boolean showWarningForNonPluginJars) {
		findPlugins(pluginDir, showWarningForNonPluginJars);
		Iterator<Plugin> i = allPlugins.iterator();
		while (i.hasNext()) {
			Plugin plugin = i.next();
			if (!plugin.checkDependencies(allPlugins)) {
				LogService.getRoot().warning("Cannot register operators from '" + plugin.getName() + "': Dependencies not fulfilled! This plugin needs a RapidMiner version " + plugin.getNecessaryRapidMinerVersion() + " and the following plugins:" + Tools.getLineSeparator() + plugin.getPluginDependencies());
				plugin.disabled = true;
				i.remove();
			}
		}

		if (allPlugins.size() > 0) {
			i = allPlugins.iterator();
			while (i.hasNext()) {
				Plugin plugin = i.next();
				try {
					plugin.registerDescriptions();
				} catch (Exception e) {
					LogService.getRoot().log(Level.WARNING, "Error initializing plugin: "+ e, e);
					i.remove();
					plugin.disabled = true;
				}
			}
		}
	}

	/**
	 * Registers all operators from the plugins previously found by a call of registerAllPluginDescriptions
	 */
	public static void registerAllPluginOperators() {
		for (Plugin plugin: allPlugins) {
			plugin.registerOperators();
		}
	}

	
	/** Returns a class loader which is able to load all classes (core _and_ all plugins). */
	public static ClassLoader getMajorClassLoader() {
		return MAJOR_CLASS_LOADER;
		// PluginClassLoader majorPluginClassLoader = null;
		// Iterator<Plugin> i = allPlugins.iterator();
		// while (i.hasNext()) {
		// Plugin plugin = i.next();
		// if (majorPluginClassLoader == null) {
		// majorPluginClassLoader = plugin.classLoader;
		// } else {
		// mergeClassLoaders(plugin.classLoader, majorPluginClassLoader);
		// }
		// }
		// if (majorPluginClassLoader == null) {
		// return ClassLoader.getSystemClassLoader();
		// } else {
		// return majorPluginClassLoader;
		// }
	}

	/** Returns the collection of all plugins. */
	public static List<Plugin> getAllPlugins() {
		return allPlugins;
	}
	/*
	/** Returns the desired plugin. 
	private static Plugin getPlugin(String name) {
		Iterator<Plugin> i = allPlugins.iterator();
		while (i.hasNext()) {
			Plugin plugin = i.next();
			if (plugin.getName().equals(name))
				return plugin;
		}
		return null;
	}
	*/
	/** Returns the plugin with the given extension id. */
	private static Plugin getPluginByExtensionId(String name) {
		Iterator<Plugin> i = allPlugins.iterator();
		while (i.hasNext()) {
			Plugin plugin = i.next();
			if (name.equals(plugin.getExtensionId()))
				return plugin;
		}
		return null;
	}

	/**
	 * This method will try to invoke the method void initGui(MainFrame) of PluginInit class of every plugin.
	 */
	public static void initPluginGuis(MainFrame mainframe) {
		callPluginInitMethods("initGui", new Class[] { MainFrame.class }, new Object[] { mainframe }, false);
	}

	/**
	 * This method will try to invoke the public static method initPlugin() of the class com.rapidminer.PluginInit for
	 * arbitrary initializations of the plugins. It is called directly after registering the plugins.
	 */
	public static void initPlugins() {
		callPluginInitMethods("initPlugin", new Class[] {}, new Object[] {}, true);
	}

	public static void initPluginUpdateManager() {
		callPluginInitMethods("initPluginManager", new Class[] {}, new Object[] {}, false);
	}

	public static void initFinalChecks() {
		callPluginInitMethods("initFinalChecks", new Class[] {}, new Object[] {}, false);
	}

	private static void callPluginInitMethods(String methodName, Class[] arguments, Object[] argumentValues, boolean useOriginalJarClassLoader) {
		List<Plugin> plugins = getAllPlugins();
		for (Plugin plugin : plugins) {
			plugin.callInitMethod(methodName, arguments, argumentValues, useOriginalJarClassLoader);
		}
	}
	
	private void callInitMethod(String methodName, Class[] arguments, Object[] argumentValues, boolean useOriginalJarClassLoader) {
		if (pluginInitClassName == null) {
			return;
		}
		try {
			ClassLoader classLoader;
			if (useOriginalJarClassLoader) {
				classLoader = getOriginalClassLoader();
			} else {
				classLoader = getClassLoader();
			}
			Class<?> pluginInitator = Class.forName(pluginInitClassName, false, classLoader);
			Method initMethod;
			try {
				initMethod = pluginInitator.getMethod(methodName, arguments);
			} catch (NoSuchMethodException e) {
				return;
			}
			initMethod.invoke(null, argumentValues);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Plugin initializer "+pluginInitClassName+"." + methodName + " of Plugin " + getName() + " causes an error: " + e.getMessage(), e);
		}
	}

	public static void initPluginSplashTexts(SplashScreen splashScreen) {
		callPluginInitMethods("initSplashTexts", new Class[] {SplashScreen.class}, new Object[] { splashScreen }, false);
	}

	public static void initAboutTexts(Properties properties) {
		callPluginInitMethods("initAboutTexts", new Class[] { Properties.class }, new Object[] { properties }, false);
	}

	public boolean showAboutBox() {
		try {
			Class<?> pluginInitator = Class.forName(pluginInitClassName, false, getOriginalClassLoader());
			Method initGuiMethod = pluginInitator.getMethod("showAboutBox", new Class[] {});
			Boolean showAboutBox = (Boolean) initGuiMethod.invoke(null, new Object[] {});
			return showAboutBox.booleanValue();
		} catch (ClassNotFoundException e) {
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		return true;
	}

	/**
	 * Initializes all plugins if {@link RapidMiner#PROPERTY_RAPIDMINER_INIT_PLUGINS} is set. Plugins are searched for
	 * in tjhe directory specified by {@link RapidMiner#PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION} or, if this is not
	 * set, in the RapidMiner/lib/plugins directory.
	 */
	public static void initAll() {
		ManagedExtension.init();
		String loadPluginsString = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS);
		boolean loadPlugins = Tools.booleanValue(loadPluginsString, true);
		if (loadPlugins) {
			File pluginDir = null;
			String pluginDirString = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION);
			if ((pluginDirString != null) && !pluginDirString.isEmpty()) {
				pluginDir = new File(pluginDirString);
			}

			if (pluginDir == null) {
				try {
					pluginDir = getPluginLocation();
				} catch (IOException e) {
					LogService.getRoot().warning("None of the properties "+RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS+" and "+Launcher.PROPERTY_RAPIDMINER_HOME+" is set. No globally installed plugins will be loaded.");
				}
			}

			if (pluginDir != null) {
				LogService.getRoot().config("Scanning plugins in " + pluginDir + ".");
				registerAllPluginDescriptions(pluginDir, true);
			}
			initPlugins();
		} else {
			LogService.getRoot().config("Plugins skipped.");
		}
	}

	/** Specifies whether plugins should be initialized on startup. */
	public static void setInitPlugins(boolean init) {
		System.setProperty(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS, Boolean.toString(init));
	}

	/** Specifies a directory to scan for plugins. */
	public static void setPluginLocation(String directory) {
		System.setProperty(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION, directory);
	}

	/** Returns the prefix to be used in the operator keys. */
	public String getPrefix() {
		return this.prefix;
	}

	public JarFile getArchive() {
		return archive;
	}
	
	public String getExtensionId() {
		return extensionId;
	}

	public static File getPluginLocation() throws IOException {
		String locationProperty = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION);
		if (locationProperty == null) {
			return ParameterService.getLibraryFile("plugins");
		} else {
			return new File(locationProperty);
		}
	}
}
