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
package com.rapidminer.gui.renderer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.operator.IOObject;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;

/**
 * The renderer service is the basic provider for all registered renderers. All {@link IOObject}s which want to provide
 * a Renderer for visualization and reporting must place an entry in the
 * <code>ioobjects.xml</xml> file in order to allow
 * for renderer retrieval.
 * 
 * @author Ingo Mierswa
 */
public class RendererService {

	private static Set<String> objectNames = new TreeSet<String>();

	private static Map<String, List<Renderer>> objectRenderers = new HashMap<String, List<Renderer>>();

	private static Map<String, Class<? extends IOObject>> objectClasses = new HashMap<String, Class<? extends IOObject>>();

	private static Map<String, Boolean> reportableMap = new HashMap<String, Boolean>();

	private static Map<Class<?>, String> class2NameMap = new HashMap<Class<?>, String>();

	public static void init() {
		URL url = Tools.getResource("ioobjects.xml");
		init(url);
		init("ioobjects.xml", url, RendererService.class.getClassLoader());
	}

	public static void init(URL ioObjectsURL) {
		init(ioObjectsURL.getFile(), ioObjectsURL, RendererService.class.getClassLoader());
	}

	public static void init(String name, InputStream in) {
		init(name, in, RendererService.class.getClassLoader());
	}

	public static void init(String name, URL ioObjectsURL, ClassLoader classLoader) {
		InputStream in = null;
		try {
			if (ioObjectsURL != null) {
				in = ioObjectsURL.openStream();
				init(name, in, classLoader);
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Cannot initialize io object description of plugin" + name + ": " + "Cannot parse document: " + e, e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
	}

	public static void init(String rendererFileName, InputStream in, ClassLoader classLoader) {
		LogService.getRoot().config("Loading renderers from '" + rendererFileName + "'.");
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
			Element ioObjectsElement = document.getDocumentElement();
			if (ioObjectsElement.getTagName().equals("ioobjects")) {
				NodeList ioObjectNodes = ioObjectsElement.getElementsByTagName("ioobject");
				for (int i = 0; i < ioObjectNodes.getLength(); i++) {
					Node ioObjectNode = ioObjectNodes.item(i);
					if (ioObjectNode instanceof Element) {
						Element ioObjectElement = (Element)ioObjectNode;

						String name = ioObjectElement.getAttribute("name");
						String className = ioObjectElement.getAttribute("class");
						String reportableString = "true";
						if (ioObjectElement.hasAttribute("reportable")) {
							reportableString = ioObjectElement.getAttribute("reportable");
						}
						boolean reportable = Tools.booleanValue(reportableString, true);

						NodeList rendererNodes = ioObjectElement.getElementsByTagName("renderer");
						List<String> renderers = new LinkedList<String>();
						for (int k = 0; k < rendererNodes.getLength(); k++) {
							Node rendererNode = rendererNodes.item(k);
							if (rendererNode instanceof Element) {
								Element rendererElement = (Element)rendererNode;
								String rendererName = rendererElement.getTextContent();
								renderers.add(rendererName);
							}
						}

						registerRenderers(name, className, reportable, renderers, classLoader);
					}
				}
			} else {
				LogService.getRoot().warning("Cannot initialize io object description: Outermost tag of a ioobjects.xml definition must be <ioobjects>!");
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Cannot initialize io object description: Cannot parse document: " + e, e);
		} catch (javax.xml.parsers.ParserConfigurationException e) {
			LogService.getRoot().log(Level.WARNING, "Cannot initialize io object description: " + e, e);
		} catch (SAXException e) {
			LogService.getRoot().log(Level.WARNING, "Cannot initialize io object description: Cannot parse document: " + e, e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}

	}

	@SuppressWarnings("unchecked")
	public static void registerRenderers(String name, String className, boolean reportable, List<String> rendererNames, ClassLoader classLoader) {
		objectNames.add(name);

		try {
			
			Class<? extends IOObject> clazz = (Class<? extends IOObject>) Class.forName(className, true, classLoader);

			List<Renderer> renderers = new LinkedList<Renderer>();
			for (String rendererName : rendererNames) {
				Class<?> rendererClass;
				try {
					rendererClass = Class.forName(rendererName, true, classLoader);
				} catch (Exception e) { // should be unnecessary in most cases, because plugin loader contains core
					// classes
					rendererClass = Class.forName(rendererName);
				}
				Renderer renderer = (Renderer) rendererClass.newInstance();
				renderers.add(renderer);
			}

			objectRenderers.put(name, renderers);
			objectClasses.put(name, clazz);
			class2NameMap.put(clazz, name);
			reportableMap.put(name, reportable);
		} catch (Throwable e) {
			LogService.getRoot().log(Level.WARNING, "Cannot register renderer: " + e, e);
		}
	}

	public static Set<String> getAllRenderableObjectNames() {
		return objectNames;
	}

	public static Set<String> getAllReportableObjectNames() {
		Set<String> result = new TreeSet<String>();
		for (String name : objectNames) {
			Boolean reportable = reportableMap.get(name);
			if ((reportable != null) && (reportable)) {
				result.add(name);
			}
		}
		return result;
	}

	public static String getName(Class<?> clazz) {
		String result = class2NameMap.get(clazz);
		if (result == null) {
			result = getNameForSuperClass(clazz);
		}
		return result;
	}

	private static String getNameForSuperClass(Class<?> clazz) {
		for (Class<?> renderable: class2NameMap.keySet()) {
			if (renderable.isAssignableFrom(clazz))
				return class2NameMap.get(renderable);
		}
		return null;
	}

	/**
	 * This returns the highest super class of the report type with the given name.
	 */
	public static Class<? extends IOObject> getClass(String name) {
		return objectClasses.get(name);
	}

	public static List<Renderer> getRenderers(String name) {
		List<Renderer> renderers = objectRenderers.get(name);
		if (renderers != null)
			return renderers;
		return new LinkedList<Renderer>();
	}

	public static Renderer getRenderer(String reportableName, String rendererName) {
		List<Renderer> renderers = getRenderers(reportableName);
		for (Renderer renderer : renderers) {
			if (renderer.getName().equals(rendererName)) {
				return renderer;
			}
		}
		return null;
	}
}
