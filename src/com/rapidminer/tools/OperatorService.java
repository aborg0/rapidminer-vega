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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.RapidMiner;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.performance.AbstractPerformanceEvaluator;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.tools.OperatorCreationHook;
import com.rapidminer.tools.documentation.OperatorDocBundle;
import com.rapidminer.tools.plugin.Plugin;

/**
 * <p>
 * This class reads the description of the RapidMiner operators. These
 * descriptions are entries in a XML File like:
 * </p>
 * <br>
 * <code>
 *    &lt;operators&gt;<br>
 *    &nbsp;&nbsp;&lt;operator<br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;name="OperatorName" <br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;class="java.path.OperatorClass" <br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;description="OperatorDescription" <br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;deprecation="OperatorDeprecationInfo" <br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;group="OperatorGroup" <br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;icon="OperatorIcon" <br> 
 *    /&gt;<br>
 *  </code><br>
 * 
 * <p>
 * The values (and the whole tag) for deprecation and icon might be omitted. If
 * no deprecation info was specified, the operator is simply not deprecated. If
 * no icon is specified, RapidMiner just uses the icon of the parent group.
 * </p>
 * 
 * <p>
 * NOTE: This class should be used to create operators and is therefore an
 * operator factory.
 * </p>
 * 
 * <p>
 * NOTE: As of RM 5.0, the description attribute is deprecated. It is replaced
 * by the corresponding elements of {@link OperatorDocBundle}.
 * </p>
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class OperatorService {

	// static final String DEFAULT_OPERATOR_DOC_RESOURCE =
	// "com.rapidminer.resources.i18n.OperatorDoc";
	// private static final String OPERATORS_XML = "operators.xml";

	public static final String RAPID_MINER_CORE_PREFIX = "RapidMiner Core";

	// static final String DEFAULT_OPERATOR_DOC_RESOURCE =
	// "com.rapidminer.resources.i18n.renamedOperatorDoc";
	private static final String OPERATORS_XML = "OperatorsCore.xml";

	/**
	 * Maps operator names of form classname|subclassname to operator
	 * descriptions.
	 */
	private static final Map<String, OperatorDescription> NAMES_TO_DESCRIPTIONS = new HashMap<String, OperatorDescription>();

	/** Set of all Operator classes registered. */
	private static final Set<Class<? extends Operator>> REGISTERED_OPERATOR_CLASSES = new HashSet<Class<? extends Operator>>();

	/** The Map for all IO objects (maps short names on classes). */
	private static final Map<String, Class<? extends IOObject>> IO_OBJECT_NAME_MAP = new TreeMap<String, Class<? extends IOObject>>();

	/** Maps deprecated operator names to new names. */
	private static final Map<String, String> DEPRECATION_MAP = new HashMap<String, String>();

	// private static Map<String,String> operatorHelpMap = new
	// HashMap<String,String>();
	// public static final OperatorDocBundle OPERATOR_HELP_BUNDLE =
	// (OperatorDocBundle)OperatorDocBundle.loadDefault();

	/** Returns the main operator description file (XML). */
	private static URL getMainOperators() {
		String resource;
		String operatorsXML = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_INIT_OPERATORS);
		if (operatorsXML != null) {
			resource = operatorsXML;
			LogService.getRoot().config("Main operator descriptor overrideen by system property. Using " + operatorsXML + ".");
		} else {
			resource = "/" + Tools.RESOURCE_PREFIX + OPERATORS_XML;
		}
		return OperatorService.class.getResource(resource);
	}

	public static void init() {
		URL mainOperators = getMainOperators();
		if (mainOperators == null) {
			LogService.getRoot().severe("Cannot find main operator description file " + Tools.RESOURCE_PREFIX + OPERATORS_XML + ".");
		} else {
			registerOperators(mainOperators, null, null);
		}

		// additional operators from starting parameter
		String additionalOperators = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_OPERATORS_ADDITIONAL);
		if ((additionalOperators != null) && !additionalOperators.isEmpty()) {
			if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
				LogService.getRoot()
						.config(
								"Execution mode " + RapidMiner.getExecutionMode() + " does not permit accessing the file system. Ignoring additional operator description files '"
										+ additionalOperators + "'.");
			} else {
				LogService.getRoot().info("Loading additional operators specified by RapidMiner.PROPERTY_RAPIDMINER_OPERATORS_ADDITIONAL (" + additionalOperators + ")");
				String[] additionalOperatorFileNames = additionalOperators.split(File.pathSeparator);
				for (int i = 0; i < additionalOperatorFileNames.length; i++) {
					File additionalOperatorFile = new File(additionalOperatorFileNames[i]);
					if (additionalOperatorFile.exists()) {
						FileInputStream in = null;
						try {
							in = new FileInputStream(additionalOperatorFile);
							OperatorService.registerOperators(additionalOperatorFile.getPath(), in, null);
						} catch (IOException e) {
							LogService.getRoot().log(Level.SEVERE, "Cannot read '" + additionalOperatorFile + "'.", e);
						} finally {
							if (in != null) {
								try {
									in.close();
								} catch (IOException e) {
								}
							}
						}
					} else {
						LogService.getRoot().severe("Cannot find operator description file '" + additionalOperatorFileNames[i] + "'");
					}
				}
			}
		}

		// loading operators from plugins
		Plugin.registerAllPluginOperators();

		LogService.getRoot().config(
				"Number of registered operator classes: " + REGISTERED_OPERATOR_CLASSES.size() + "; number of registered operator descriptions: " + NAMES_TO_DESCRIPTIONS.size()
						+ "; number of replacements: " + DEPRECATION_MAP.size());
	}

	public static void registerOperators(URL operatorsXML, ClassLoader classLoader, Plugin plugin) {
		InputStream inputStream;
		try {
			inputStream = operatorsXML.openStream();
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Cannot open stream to operator description file " + operatorsXML + ": " + e, e);
			return;
		}
		registerOperators(OPERATORS_XML, inputStream, null, plugin);
	}

	/**
	 * Registers all operators from a given XML input stream. Closes the stream.
	 */
	public static void registerOperators(String name, InputStream operatorsXML, ClassLoader classLoader) {
		registerOperators(name, operatorsXML, classLoader, null);
	}

	public static void registerOperators(String name, InputStream operatorsXML, ClassLoader classLoader, Plugin provider) {
		// register operators
		if (classLoader == null) {
			classLoader = OperatorService.class.getClassLoader();
		}
		LogService.getRoot().config("Loading operators from '" + name + "'.");
		String version = null;
		Document document = null;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(operatorsXML);
			if (!document.getDocumentElement().getTagName().toLowerCase().equals("operators")) {
				LogService.getRoot().severe("Operator description file '" + name + "': outermost tag must be <operators>!");
				return;
			}
			version = document.getDocumentElement().getAttribute("version");
			if (version.equals("5.0")) {
				parseOperators(document, classLoader, provider);
			} else {
				parseOperatorsPre5(document, classLoader, provider);
			}
		} catch (Exception e) {
			LogService.getRoot().log(Level.SEVERE, "Cannot read operator description file '" + name + "': no valid XML: " + e.getMessage(), e);
			return;
		} finally {
			try {
				operatorsXML.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void parseOperators(Document document, ClassLoader classLoader, Plugin provider) throws XMLException, OperatorCreationException {
		String docBundle = document.getDocumentElement().getAttribute("docbundle");
		OperatorDocBundle bundle;
		if ((docBundle == null) || docBundle.isEmpty()) {
			bundle = null;
			LogService.getRoot().warning("Operators for " + provider.getName() + " don't have an attached documentation.");
		} else {
			bundle = OperatorDocBundle.load(classLoader, docBundle);
		}

		parseOperators(GroupTree.ROOT, document.getDocumentElement(), classLoader, provider, bundle);
	}

	private static void parseOperators(GroupTree currentGroup, Element groupElement, ClassLoader classLoader, Plugin provider, OperatorDocBundle bundle) throws XMLException, OperatorCreationException {
		NodeList children = groupElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element childElement = (Element) child;
				if (childElement.getTagName().equals("group")) {
					String name = childElement.getAttribute("key");
					String icon = XMLTools.getTagContents(childElement, "icon");
					GroupTree newTree;
					if ((name != null) && !name.isEmpty()) {
						newTree = currentGroup.getOrCreateSubGroup(name, bundle);
					} else {
						newTree = currentGroup;
					}
					if (icon != null && icon.length() > 0) {
						newTree.setIconName(icon);
					} else {
						if (newTree.getIconName() == null || newTree.getIconName().length() == 0)
							newTree.setIconName(currentGroup.getIconName());
					}
					parseOperators(newTree, childElement, classLoader, provider, bundle);
				} else if (childElement.getTagName().equals("operator")) {
					try {
						OperatorDescription desc = new OperatorDescription(currentGroup, childElement, classLoader, provider, bundle);
						registerOperator(desc);
						if (desc.getReplacedKeys() != null) {
							for (String replaces : desc.getReplacedKeys()) {
								DEPRECATION_MAP.put(replaces, desc.getKey());
							}
						}
					} catch (ClassNotFoundException e) {
						LogService.getRoot().log(Level.WARNING, "Cannot load operator class: " + e, e);
					} catch (NoClassDefFoundError e) {
						LogService.getRoot().log(Level.WARNING, "Cannot load operator class: " + e, e);
					}
				} else if (childElement.getTagName().equals("factory")) {
					String factoryClassName = childElement.getTextContent();
					if ((factoryClassName == null) || (factoryClassName.isEmpty())) {
						LogService.getRoot().warning("Malformed operator descriptor: <factory> tag must contain class name!");
					} else {
						Class factoryClass = null;
						try {
							factoryClass = Class.forName(factoryClassName, true, classLoader);
						} catch (ClassNotFoundException e) {
							LogService.getRoot().warning("Operator factory class '" + factoryClassName + "' not found!");
						}
						if (factoryClass != null) {
							if (GenericOperatorFactory.class.isAssignableFrom(factoryClass)) {
								GenericOperatorFactory factory = null;
								try {
									factory = (GenericOperatorFactory) factoryClass.newInstance();
								} catch (Exception e) {
									LogService.getRoot().warning("Cannot instantiate operator factory class '" + factoryClass.getName() + "'!");
								}
								LogService.getRoot().config("Creating operators from factory " + factoryClassName);
								try {
									factory.registerOperators(classLoader, provider);
								} catch (Exception e) {
									LogService.getRoot().log(Level.WARNING, "Error registering operators from "+factoryClass.getName()+e, e);
								}
							} else {
								LogService.getRoot().warning("Malformed operator descriptor: Only subclasses of GenericOperatorFactory may be defined as class, was '" + factoryClassName + "'!");
							}
						}
					}
				} else if (childElement.getTagName().equals("icon")) {
					// why do we ignore this?
				} else {
					throw new XMLException("Illegal tag in operator descrioption file: " + childElement.getTagName());
				}
			}
		}
	}

	private static void parseOperatorsPre5(Document document, ClassLoader classLoader, Plugin provider) {
		// operators
		NodeList operatorTags = document.getDocumentElement().getElementsByTagName("operator");
		for (int i = 0; i < operatorTags.getLength(); i++) {
			Element currentElement = (Element) operatorTags.item(i);
			try {
				parseOperatorPre5(currentElement, classLoader, provider);
			} catch (Throwable e) {
				Attr currentNameAttr = currentElement.getAttributeNode("name");
				if (currentNameAttr != null) {
					LogService.getRoot().log(Level.WARNING, "Cannot register '" + currentNameAttr.getValue() + "': " + e, e);
				} else {
					LogService.getRoot().log(Level.WARNING, "Cannot register '" + currentElement + "': " + e, e);
				}
			}
		}
	}

	/**
	 * Registers an operator description from an XML tag (operator description
	 * file, mostly operators.xml).
	 * 
	 * Warning suppressed because of old style creation of OperatorDescription
	 */
	@SuppressWarnings("deprecation")
	private static void parseOperatorPre5(Element operatorTag, ClassLoader classLoader, Plugin provider) throws Exception {
		Attr nameAttr = operatorTag.getAttributeNode("name");
		Attr classAttr = operatorTag.getAttributeNode("class");
		if (nameAttr == null)
			throw new Exception("Missing name for <operator> tag");
		if (classAttr == null)
			throw new Exception("Missing class for <operator> tag");

		String name = nameAttr.getValue();

		String deprecationString = operatorTag.getAttribute("deprecation");
		String group = operatorTag.getAttribute("group");
		String icon = operatorTag.getAttribute("icon");
		if (icon.isEmpty()) {
			icon = null;
		}

		String names[] = name.split(",");
		int i = 0;
		for (String opName : names) {
			if (i > 0) {
				deprecationString = "Replaced by " + names[0] + ".";
				DEPRECATION_MAP.put(opName, names[0]);

				OperatorDescription replacement = getOperatorDescription(names[0]);
				replacement.setIsReplacementFor(opName);
			}
			String name1 = opName.trim();
			OperatorDescription description = new OperatorDescription(classLoader, name1, name1, classAttr.getValue(), group, icon, deprecationString, provider);
			// add to group
			registerOperator(description);
			i++;
		}
	}

	/**
	 * Registers the given operator description. Please note that two different
	 * descriptions must not have the same name. Otherwise the second
	 * description overwrite the first in the description map.
	 * 
	 * @throws OperatorCreationException
	 */
	public static void registerOperator(OperatorDescription description) throws OperatorCreationException {
		// check if this operator was not registered earlier
		OperatorDescription oldDescription = NAMES_TO_DESCRIPTIONS.get(description.getName());
		if (oldDescription != null) {
			LogService.getRoot().warning(
					"Operator key '" + description.getKey() + "' was already registered for class " + oldDescription.getOperatorClass().getName() + ". Overwriting with "
							+ description.getOperatorClass() + ".");
		}

		// register
		NAMES_TO_DESCRIPTIONS.put(description.getKey(), description);
		REGISTERED_OPERATOR_CLASSES.add(description.getOperatorClass());

		Operator currentOperator = description.createOperatorInstance();
		currentOperator.assumePreconditionsSatisfied();
		currentOperator.transformMetaData();
		checkIOObjects(currentOperator.getInputPorts());
		checkIOObjects(currentOperator.getOutputPorts());
	}

	/**
	 * Checks if the classes generated by these ports are already registered and
	 * registers them if not.
	 */
	private static void checkIOObjects(Ports<? extends Port> ports) {
		List<Class<? extends IOObject>> result = new LinkedList<Class<? extends IOObject>>();
		for (Port port : ports.getAllPorts()) {
			if (port.getMetaData() != null) {
				result.add(port.getMetaData().getObjectClass());
			}
		}
		registerIOObjects(result);
	}

	/** Checks if the given classes are already registered and adds them if not. */
	public static void registerIOObjects(Collection<Class<? extends IOObject>> objects) {
		for (Class<? extends IOObject> currentClass : objects) {
			String current = currentClass.getName();
			IO_OBJECT_NAME_MAP.put(current.substring(current.lastIndexOf(".") + 1), currentClass);
		}
	}

	/** Returns a sorted set of all short IO object names. */
	public static Set<String> getIOObjectsNames() {
		return IO_OBJECT_NAME_MAP.keySet();
	}

	/**
	 * Defines the alias pairs for the {@link XMLSerialization} for all IOObject
	 * pairs.
	 */
	public static void defineXMLAliasPairs() {
		// pairs for IOObjects
		Iterator<Map.Entry<String, Class<? extends IOObject>>> i = IO_OBJECT_NAME_MAP.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<String, Class<? extends IOObject>> entry = i.next();
			String objectName = entry.getKey();
			Class objectClass = entry.getValue();
			XMLSerialization.getXMLSerialization().addAlias(objectName, objectClass);
		}

		// pairs for performance criteria
		Iterator<String> o = getOperatorNames().iterator();
		while (o.hasNext()) {
			String name = o.next();
			OperatorDescription description = getOperatorDescription(name);
			// test if operator delivers performance criteria
			if (AbstractPerformanceEvaluator.class.isAssignableFrom(description.getOperatorClass())) {
				Operator operator = null;
				try {
					operator = createOperator(name);
				} catch (OperatorCreationException e) {
					// does nothing
				}
				if (operator != null) {
					AbstractPerformanceEvaluator evaluator = (AbstractPerformanceEvaluator) operator;
					List<PerformanceCriterion> criteria = evaluator.getCriteria();
					for (PerformanceCriterion criterion : criteria) {
						XMLSerialization.getXMLSerialization().addAlias(criterion.getName(), criterion.getClass());
					}
				}
			}
		}
	}

	/*
	 * /** Returns a collection of all operator descriptions of operators which
	 * return the desired IO object as output. TODO: Remove. This method is
	 * never called.
	 * 
	 * @deprecated This method is never called.
	 * 
	 * @Deprecated public static Set<OperatorDescription>
	 * getOperatorsDelivering(Class<? extends IOObject> ioObject) {
	 * Set<OperatorDescription> result = new HashSet<OperatorDescription>();
	 * Iterator<String> i = NAMES_TO_DESCRIPTIONS.keySet().iterator(); while
	 * (i.hasNext()) { String name = i.next(); OperatorDescription description =
	 * getOperatorDescription(name); try { Operator currentOperator =
	 * description.createOperatorInstance(); if
	 * (currentOperator.producesOutput(ioObject)) { result.add(description); } }
	 * catch (Exception e) { LogService.getRoot().log(Level.WARNING,
	 * "Cannot check IO for operator "+name+": "+e, e); } } return result; }
	 * 
	 * /** Returns a collection of all operator descriptions which requires the
	 * given IO object as input. TODO: Remove. Method is never called.
	 * 
	 * @deprecated Method is never called.
	 * 
	 * @Deprecated public static Set<OperatorDescription>
	 * getOperatorsRequiring(Class<? extends IOObject> ioObject) {
	 * Set<OperatorDescription> result = new HashSet<OperatorDescription>();
	 * Iterator<String> i = NAMES_TO_DESCRIPTIONS.keySet().iterator(); while
	 * (i.hasNext()) { String name = i.next(); OperatorDescription description =
	 * getOperatorDescription(name); try { Operator currentOperator =
	 * description.createOperatorInstance(); if
	 * (currentOperator.acceptsInput(ioObject)) { result.add(description); } }
	 * catch (Exception e) {} } return result; }
	 */

	/** Returns the class for the short name of an IO object. */
	public static Class<? extends IOObject> getIOObjectClass(String name) {
		return IO_OBJECT_NAME_MAP.get(name);
	}

	/**
	 * Returns a collection of all operator names. A name has the structure
	 * classname|subclassname.
	 */
	public static Set<String> getOperatorNames() {
		return NAMES_TO_DESCRIPTIONS.keySet();
	}

	/** Returns the group hierarchy of all operators. */
	public static GroupTree getGroups() {
		return GroupTree.ROOT;
	}

	// ================================================================================
	// Operator Factory Methods
	// ================================================================================

	/**
	 * Returns the operator descriptions for the operators which uses the given
	 * class. Performs a linear seach through all operator descriptions.
	 */
	public static OperatorDescription[] getOperatorDescriptions(Class clazz) {
		List<OperatorDescription> result = new LinkedList<OperatorDescription>();
		for (OperatorDescription current : NAMES_TO_DESCRIPTIONS.values()) {
			if (current.getOperatorClass().equals(clazz))
				result.add(current);
		}
		OperatorDescription[] resultArray = new OperatorDescription[result.size()];
		result.toArray(resultArray);
		return resultArray;
	}

	/**
	 * Returns the operator description for a given class name from the
	 * operators.xml file, e.g. &quot;Process&quot; for a ProcessRootOperator.
	 */
	public static OperatorDescription getOperatorDescription(String completeName) {
		return NAMES_TO_DESCRIPTIONS.get(completeName);
	}

	/**
	 * Use this method to create an operator from the given class name (from
	 * operator description file operators.xml, not from the Java class name).
	 * For most operators, is is recommended to use the method
	 * {@link #createOperator(Class)} which can be checked during compile time.
	 * This is, however, not possible for some generic operators like the Weka
	 * operators. In that case, you have to use this method with the argument
	 * from the operators.xml file, e.g.
	 * <tt>createOperator(&quot;J48&quot;)</tt> for a J48 decision tree learner.
	 */
	public static Operator createOperator(String typeName) throws OperatorCreationException {
		OperatorDescription description = getOperatorDescription(typeName);
		if (description == null)
			throw new OperatorCreationException(OperatorCreationException.NO_DESCRIPTION_ERROR, typeName, null);
		return createOperator(description);
	}

	/** Use this method to create an operator of a given description object. */
	public static Operator createOperator(OperatorDescription description) throws OperatorCreationException {
		return description.createOperatorInstance();
	}

	/**
	 * <p>
	 * Use this method to create an operator from an operator class. This is the
	 * only method which ensures operator existence checks during compile time
	 * (and not during runtime) and the usage of this method is therefore the
	 * recommended way for operator creation.
	 * </p>
	 * 
	 * <p>
	 * It is, however, not possible to create some generic operators with this
	 * method (this mainly applies to the Weka operators). Please use the method
	 * {@link #createOperator(String)} for those generic operators.
	 * </p>
	 * 
	 * <p>
	 * If you try to create a generic operator with this method, the
	 * OperatorDescription will not be unique for the given class and an
	 * OperatorCreationException is thrown.
	 * </p>
	 * 
	 * <p>
	 * Please note that is is not necessary to cast the operator to the desired
	 * class.
	 * </p>
	 * 
	 * TODO: can we remove the suppress warning here?
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Operator> T createOperator(Class<T> clazz) throws OperatorCreationException {
		OperatorDescription[] descriptions = getOperatorDescriptions(clazz);
		if (descriptions.length == 0) {
			throw new OperatorCreationException(OperatorCreationException.NO_DESCRIPTION_ERROR, clazz.getName(), null);
		} else if (descriptions.length > 1) {
			List<OperatorDescription> nonDeprecated = new LinkedList<OperatorDescription>();
			for (OperatorDescription od : descriptions) {
				if (od.getDeprecationInfo() == null) {
					nonDeprecated.add(od);
				}
			}
			if (nonDeprecated.size() > 1) {
				throw new OperatorCreationException(OperatorCreationException.NO_UNIQUE_DESCRIPTION_ERROR, clazz.getName(), null);
			} else {
				return (T) nonDeprecated.get(0).createOperatorInstance();
			}
		} else {
			return (T) descriptions[0].createOperatorInstance();
		}
	}

	/**
	 * Returns a replacement if the given operator class is deprecated, and null
	 * otherwise.
	 */
	public static String getReplacementForDeprecatedClass(String deprecatedClass) {
		return DEPRECATION_MAP.get(deprecatedClass);
	}

	/** Specifies a list of files to be loaded as operator descriptors. */
	public static void setAdditionalOperatorDescriptors(String... files) {
		if ((files == null) || (files.length == 0)) {
			System.setProperty(RapidMiner.PROPERTY_RAPIDMINER_OPERATORS_ADDITIONAL, null);
		}
		StringBuffer buf = new StringBuffer();
		boolean first = true;
		for (String file : files) {
			if (!first) {
				buf.append(File.pathSeparator);
			} else {
				first = false;
			}
			buf.append(file);
		}
		System.setProperty(RapidMiner.PROPERTY_RAPIDMINER_OPERATORS_ADDITIONAL, buf.toString());
	}

	private static final List<OperatorCreationHook> operatorCreationHooks = new LinkedList<OperatorCreationHook>();

	public static void addOperatorCreationHook(OperatorCreationHook operatorCreationHook) {
		operatorCreationHooks.add(operatorCreationHook);
	}

	public static void invokeCreationHooks(Operator operator) {
		for (OperatorCreationHook hook : operatorCreationHooks) {
			try {
				hook.operatorCreated(operator);
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, "Error in operator creation hooK: " + e, e);
			}
		}
	}
}
