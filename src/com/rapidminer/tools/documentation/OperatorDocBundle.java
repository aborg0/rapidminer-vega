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
package com.rapidminer.tools.documentation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.XMLException;

/** A resource bundle that maps operator names to {@link OperatorDocumentation} instances.
 *  Instances of this class always return {@link OperatorDocumentation}s from their
 *  {@link #getObject(String)} methods.
 *  
 *  The XML structure of the documentation is as follows: For every operator there is a tag
 *  <pre>
 *  &lt;operator&gt;
 *    &lt;synopsis&gt;SYNOPSIS&lt;/synopsis&gt;
 *    &lt;help&gt;LONG HELP TEXT&lt;/help&gt;
 *    &lt;example&gt;
 *      &lt;process&gt;XML process string&lt;/process&gt;
 *      &lt;comment&gt;COMMENT&lt;/comment&gt;
 *    &lt;/example&gt;
 *    &lt;example&gt;
 *       ...
 *    &lt;/example&gt;
 *  &lt;/operator&gt;
 *  </pre>
 * @author Simon Fischer
 *
 */
public class OperatorDocBundle extends ResourceBundle {

	/** Control to load XML files. Code is largely stolen from the javadoc of
	 *  {@link Control}.
	 *  
	 * @author Simon Fischer
	 *
	 */
	private static class XMLControl extends Control {
		@Override
		public List<String> getFormats(String baseName) {
			if (baseName == null) {
				throw new NullPointerException("baseName is null.");
			}
			return Arrays.asList("xml");
		}

		@Override
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
			if ((baseName == null) || (locale == null) || (format == null) || (loader == null)) {
				throw new NullPointerException();
			}
			LogService.getRoot().fine("Looking up operator documentation for "+baseName+", locale "+locale+".");
			if (format.equals("xml")) {
				String bundleName = toBundleName(baseName, locale);
				String resourceName = toResourceName(bundleName, format);
				URL url = loader.getResource(resourceName);
				if (url != null) {
					LogService.getRoot().config("Loading operator documentation from "+url+".");
					try {
						return new OperatorDocBundle(url, resourceName);
					} catch (Exception e) {
						LogService.getRoot().log(Level.WARNING, "Exception creating OperatorDocBundle: "+e, e);	
						return null;
					}
				}		
			}
			return null;
		}
	}

	private final Map<String,OperatorDocumentation> operatorKeyDescriptionMap = new HashMap<String,OperatorDocumentation>();

	private final Map<String, GroupDocumentation> groupMap = new HashMap<String,GroupDocumentation>();
	
	/** The XML document representing the documentation data. */
	private Document document;

	private final URL sourceUrl;
	private final String resourceName;

	/** Constructs a new OperatorDocBundle
	 * 
	 * @param url The URL from which we are reading.
	 * @param resourceName The original resource name. This is the last part of the path of the URL and will be used to locate the source file,
	 *   when this bundle is saved.
	 * @throws IOException
	 */
	public OperatorDocBundle(URL url, String resourceName) throws IOException {
		this.sourceUrl = url;
		this.resourceName = resourceName;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url.openStream());
		} catch (SAXException e) {
			throw new IOException("Malformed XML operator help bundle: "+e, e);
		} catch (ParserConfigurationException e) {
			LogService.getRoot().log(Level.WARNING, "Cannot create XML parser: "+e, e);
			return;
		}
		NodeList helpElements = document.getDocumentElement().getElementsByTagName("operator");
		for (int i = 0; i < helpElements.getLength(); i++) {
			Element element = (Element)helpElements.item(i);			
			OperatorDocumentation oh = new OperatorDocumentation(this, element);
			try {
				String operatorKey = XMLTools.getTagContents(element, "key", false);
				if (operatorKey == null) {					
					operatorKey = XMLTools.getTagContents(element, "name", true);
					LogService.getRoot().fine("Operator help is missing <key> tag. Using <name> as <key>: "+operatorKey);
				}
				operatorKeyDescriptionMap.put(operatorKey, oh);
			} catch (XMLException e) {
				LogService.getRoot().log(Level.WARNING, "Malformed operoator documentation: "+e, e);
			}
		}
		
		NodeList groupElements = document.getDocumentElement().getElementsByTagName("group");
		for (int i = 0; i < groupElements.getLength(); i++) {
			Element element = (Element)groupElements.item(i);
			GroupDocumentation doc = new GroupDocumentation(element);
			groupMap.put(doc.getKey(), doc);
		}
		
		LogService.getRoot().fine("Loaded documentation for "+operatorKeyDescriptionMap.size() +" operators and " + groupMap.size() + " groups.");
	}

	@Override
	public Enumeration<String> getKeys() {
		return Collections.enumeration(operatorKeyDescriptionMap.keySet());
	}

	@Override
	protected Object handleGetObject(String key) {
		if (key.startsWith("operator.")) {
			key = key.substring("operator.".length());
			OperatorDocumentation doc = operatorKeyDescriptionMap.get(key);
			if (doc == null) {
				Element element = document.createElement("operator");
				doc = new OperatorDocumentation(this, element);
				XMLTools.setTagContents(element, "key", key);
				document.getDocumentElement().appendChild(element);			
				operatorKeyDescriptionMap.put(key, doc);
				LogService.getRoot().fine("Creating new empty documentation for operator "+key);
			}
			return doc;
		} else if (key.startsWith("group.")) {
			key = key.substring("group.".length());			
			GroupDocumentation groupDocumentation = groupMap.get(key);
			if (groupDocumentation == null) {
				Element element = document.createElement("group");
				XMLTools.setTagContents(element, "key", key);
				String name = GroupDocumentation.keyToUpperCase(key);
				XMLTools.setTagContents(element, "name", name);
				document.getDocumentElement().appendChild(element);
				
				groupDocumentation = new GroupDocumentation(element);
				groupMap.put(key, groupDocumentation);
				LogService.getRoot().fine("Creating new empty documentation for group "+key);
			}
			return groupDocumentation;
		} else {
			return null;
		}
	}

	/** Loads the default "OperatorDoc.xml" file from the given resource base name. */
	public static OperatorDocBundle load(ClassLoader classLoader, String resource) {
		return (OperatorDocBundle) ResourceBundle.getBundle(resource, Locale.getDefault(), classLoader, new XMLControl());
	}

	/** Saves the bundle to the source directory. */
	public void save() {		
		if (!sourceUrl.getProtocol().equals("file")) {
			LogService.getRoot().warning("Cannot save operator help. Operator help was not loaded from a file, but from "+sourceUrl.getProtocol()+".");
		} else {			
			OutputStream fos = null;
			try {
				if (sourceUrl.getProtocol().equals("file")) {
					File buildFile = new File(sourceUrl.toURI());
					LogService.getRoot().info("Saving operator help file to "+buildFile);
					fos = new FileOutputStream(buildFile);
					XMLTools.stream(document, fos, null);
				} else {
					LogService.getRoot().info("Cannot save resources to "+sourceUrl+": Not a file. Saving resources only works with development builds of RapidMiner.");
				}				
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, "Cannot save operator documentation: "+e, e);
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e1) { }
				}
			}

			File sourceFile = ParameterService.getSourceResourceFile(this.resourceName);
			if (!sourceFile.exists()) {
				LogService.getRoot().info("Cannot save resource to file "+sourceFile+": File does not exist. Saving resources only works with development builds of RapidMiner.");
			} else {
				LogService.getRoot().info("Saving operator help bundle to "+sourceFile);
				OutputStream sos = null;
				try {
					sos = new FileOutputStream(sourceFile);
					XMLTools.stream(document, sos, null);
					sos.close();
				} catch (Exception e) {
					LogService.getRoot().log(Level.WARNING, "Cannot save resources to "+sourceUrl+": "+e, e);
				} finally {
					if (sos != null) {
						try {
							sos.close();
						} catch (IOException e) { }
					}
				}				
			}
		}
	}
	
	/** Checks for empty documentation and documentation that has no associated operator. */
	public void check() {
		LogService.getRoot().info("Checking operator documentation");
		int missing = 0;
		int same = 0;
		int deprecation = 0;
		int different = 0;
		int empty = 0;
		for (Map.Entry<String,OperatorDocumentation> entry : operatorKeyDescriptionMap.entrySet()) {
			String key = entry.getKey();
			OperatorDocumentation doc = entry.getValue();
			if (key.startsWith("W-")) {
				continue;
			}
			if (doc.getDocumentation().trim().isEmpty()) {
				LogService.getRoot().warning("Empty documentation for "+key);
				empty++;
			}
			OperatorDescription desc = OperatorService.getOperatorDescription(key);
			if (desc == null) {
				missing++;
				LogService.getRoot().warning("Documentation for nonexistent operator "+key);				
			}
			String replacement = OperatorService.getReplacementForDeprecatedClass(key);
			if (replacement != null) {
				deprecation++;
				String string;
				OperatorDocumentation otherDoc = operatorKeyDescriptionMap.get(replacement); 
				if (otherDoc != null) {
					if (otherDoc.getDocumentation().equals(doc.getDocumentation())) {
						string = replacement + " has the same documentation entry.";
						same++;
					} else {
						string = replacement + " has a different documentation entry.";
						different++;
					}
				} else {
					string = replacement + " has no documentation entry.";					
				}
				LogService.getRoot().warning("Documentation for deprecated operator "+key+" replaced by "+replacement+". "+string);
				
			}
		}
		LogService.getRoot().info("Found "+empty+" empty documentations. Found documentation for "+missing+" nonexistent and "+(deprecation)+" replaced operators. Out of these, "+same+" documentations are identical to the documentation of the replacement and "+(deprecation-same-different)+" replacements have no documentation.");
	}
	
	public Document getDOMRepresentation() {
		return document;
	}
}
