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
package com.rapidminer.io.process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;

/**
 * 
 * @author Sebastian Land
 */
public class XMLTools {

	private static final Map<URI, Validator> VALIDATORS = new HashMap<URI, Validator>();

	private final static DocumentBuilder BUILDER;

	public static final String SCHEMA_URL_PROCESS = "http://www.rapidminer.com/xml/schema/RapidMinerProcess";
	static {
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		DocumentBuilder builder;
		try {
			builder = domFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			builder = null;
		}
		BUILDER = builder;
	}

	private static Validator getValidator(URI schemaURI) {
		if (schemaURI == null) {
			throw new NullPointerException("SchemaURL is null!");
		}
		synchronized (VALIDATORS) {
			if (VALIDATORS.containsKey(schemaURI)) {
				return VALIDATORS.get(schemaURI);
			} else {
				SchemaFactory factory = null;
				if (factory == null) {
					throw new RuntimeException("XMLConstants.W3C_XML_SCHEMA_NS_URI cannot be resolved at compile time for JBoss.");
					// SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
				}
				Validator validator;
				try {
					validator = factory.newSchema(schemaURI.toURL()).newValidator();
				} catch (SAXException e1) {
					validator = null;
					// throw new XMLException("Cannot parse XML schema: "+e1, e1);
				} catch (MalformedURLException e) {
					validator = null;
				}
				VALIDATORS.put(schemaURI, validator);
				return validator;
			}
		}
	}

	/**
	 * This method should not be called since it is slower than {@link #parseAndValidate(InputStream, URI, String)}
	 */
	public static Document parseAndValidate(InputStream in, URL schemaURL, String sourceName) throws XMLException, IOException {
		try {
			return parseAndValidate(in, new URI(schemaURL.toString()), sourceName);
		} catch (URISyntaxException e) {
			throw new XMLException("Could not resolve URL.", e);
		}
	}

	/**
	 * The schema URL might be given as URI for performance reasons. 
	 */
	public static Document parseAndValidate(InputStream in, URI schemaURL, String sourceName) throws XMLException, IOException {
		XMLErrorHandler errorHandler = new XMLErrorHandler(sourceName);

		Document doc;
		try {
			doc = BUILDER.parse(in);
		} catch (SAXException e) {
			throw new XMLException(errorHandler.toString(), e);
		}

		Source source = new DOMSource(doc);
		DOMResult result = new DOMResult();
		Validator validator = getValidator(schemaURL);
		validator.setErrorHandler(errorHandler);
		try {
			validator.validate(source, result);
		} catch (SAXException e) {
			throw new XMLException(errorHandler.toString(), e);
		}
		if (errorHandler.hasErrors()) {
			throw new XMLException(errorHandler.toString());
		}
		return (Document) result.getNode();
	}

	public static Document parse(InputStream in) throws SAXException, IOException {
		return BUILDER.parse(in);
	}

	public static Document parse(File file) throws SAXException, IOException {
		return BUILDER.parse(file);
	}

	public static String toString(Document document, Charset encoding) throws XMLException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		StreamResult result = new StreamResult(new OutputStreamWriter(buf));
		stream(document, result, encoding);
		return buf.toString();
	}

	public static void stream(Document document, File file, Charset encoding) throws XMLException {

		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			stream(document, out, encoding);
		} catch (IOException e) {
			throw new XMLException("Cannot save XML to " + file + ": " + e, e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static void stream(Document document, OutputStream out, Charset encoding) throws XMLException {
		// we wrap this in a Writer to fix a Java bug
		// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
		if (encoding == null) {
			encoding = Charset.forName("UTF-8");
		}
		stream(document, new StreamResult(new OutputStreamWriter(out, encoding)), encoding);
	}

	public static void stream(Document document, Result result, Charset encoding) throws XMLException {
		Transformer transformer;
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			try {
				tf.setAttribute("indent-number", Integer.valueOf(2));
			} catch (IllegalArgumentException e) {
				LogService.getRoot().log(Level.WARNING, "XML transformer does not support indentation: " + e);
			}
			transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			if (encoding != null) {
				transformer.setOutputProperty(OutputKeys.ENCODING, encoding.name());
			}
		} catch (TransformerConfigurationException e) {
			throw new XMLException("Cannot transform XML: " + e, e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new XMLException("Cannot transform XML: " + e, e);
		}
		try {
			transformer.transform(new DOMSource(document), result);
		} catch (TransformerException e) {
			throw new XMLException("Cannot transform XML: " + e, e);
		}
	}

	/** As {@link #getTagContents(Element, String, boolean)}, but never throws an exception. */
	public static String getTagContents(Element element, String tag) {
		try {
			return getTagContents(element, tag, false);
		} catch (XMLException e) {
			// cannot happen
			return null;
		}
	}

	/** For a tag <tag>content</tag> returns content. */
	public static String getTagContents(Element element, String tag, boolean throwExceptionOnError) throws XMLException {
		NodeList nodeList = element.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if ((node instanceof Element) && ((Element) node).getTagName().equals(tag)) {
				Element child = (Element) node;
				return child.getTextContent();
			}
		}
		if (throwExceptionOnError) {
			throw new XMLException("Missing tag: <" + tag + "> in <" + element.getTagName() + ">.");
		} else {
			return null;
		}
	}

	/**
	 * If parent has a direct child with the given name, the child's children are removed and are replaced by a single
	 * text node with the given text. If no direct child of parent with the given tag name exists, a new one is created.
	 */
	public static void setTagContents(Element parent, String tagName, String value) {
		if (value == null) {
			value = "";
		}
		Element child = null;
		NodeList list = parent.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node instanceof Element) {
				if (((Element) node).getTagName().equals(tagName)) {
					child = (Element) node;
					break;
				}
			}
		}
		if (child == null) {
			child = parent.getOwnerDocument().createElement(tagName);
			parent.appendChild(child);
		} else {
			while (child.hasChildNodes()) {
				child.removeChild(child.getFirstChild());
			}
		}
		child.appendChild(parent.getOwnerDocument().createTextNode(value));
	}

	public static void deleteTagContents(Element annotationsElement, String name) {
		NodeList children = annotationsElement.getElementsByTagName(name);
		for (int i = 0; i < children.getLength(); i++) {
			Element child = (Element) children.item(i);
			annotationsElement.removeChild(child);
		}
	}

	public static XMLGregorianCalendar getXMLGregorianCalendar(Date date) throws DatatypeConfigurationException {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(date.getTime());
		DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
		XMLGregorianCalendar xmlGregorianCalendar = datatypeFactory.newXMLGregorianCalendar();
		xmlGregorianCalendar.setYear(calendar.get(Calendar.YEAR));
		xmlGregorianCalendar.setMonth(calendar.get(Calendar.MONTH) + 1);
		xmlGregorianCalendar.setDay(calendar.get(Calendar.DAY_OF_MONTH));
		xmlGregorianCalendar.setHour(calendar.get(Calendar.HOUR_OF_DAY));
		xmlGregorianCalendar.setMinute(calendar.get(Calendar.MINUTE));
		xmlGregorianCalendar.setSecond(calendar.get(Calendar.SECOND));
		return xmlGregorianCalendar;
	}

	public static Element getUniqueInnerTag(Element element, String tagName) throws XMLException {
		return getUniqueInnerTag(element, tagName, true);
	}

	/**
	 * This method will return null if the element doesn't exist if obligatory is false. Otherwise
	 * an exception is thrown.
	 * If the element is not unique, an exception is thrown in any cases.
	 */
	public static Element getUniqueInnerTag(Element element, String tagName, boolean obligatory) throws XMLException {
		NodeList children = element.getElementsByTagName(tagName);
		switch (children.getLength()) {
		case 0:
			if (obligatory)
				throw new XMLException("Missing inner tag <" + tagName + "> inside <" + element.getTagName() + ">.");
			else
				return null;
		case 1:
			return (Element) children.item(0);
		default:
			throw new XMLException("Inner tag <" + tagName + "> inside <" + element.getTagName() + "> must be unique, but found " + children.getLength() + ".");
		}

	}

	/**
	 * This method will return a Collection of all Elements that are direct child elements of the given element and have
	 * the given tag name.
	 */
	public static Collection<Element> getChildElements(Element father, String tagName) {
		LinkedList<Element> elements = new LinkedList<Element>();
		NodeList list = father.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node instanceof Element) {
				if (node.getNodeName().equals(tagName))
					elements.add((Element) node);
			}
		}
		return elements;
	}

}
