package com.rapidminer.gui;

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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.xml.XHTMLEntityResolver;
import common.Logger;

/**
 * A custom Bot which gets all operator description sites from a given MediaWiki
 * URL. The html files for an operator are parsed and saved in the resources
 * folder under "documentation/namespace/operatorname". If the user does not
 * have an internet connection all operators are loaded from resources folder.
 * If the user does have an internet connection the operators are loaded
 * directly from the RapidWiki site. The operator description is shown in the
 * RapidMiner Help Window.
 * 
 * @author Miguel Büscher
 * 
 */
public class OperatorDocImporter {
	private static final long serialVersionUID = 1L;

	private static final String HOST_NAME = "http://www.rapid-i.com";
	private static final String IMAGE_DIR_NAME = "images";
	private static final String IMAGE_FORMAT = "jpg";
	private static final String WIKI_PREFIX_FOR_IMAGES = "http://www.rapid-i.com";;
	private static final String WIKI_PREFIX_FOR_OPERATORS = "http://rapid-i.com/wiki/index.php?title=";

	private static final Logger logger = Logger.getLogger(OperatorDocImporter.class);
	private static String CORRECT_HTML_STRING_DIRTY = "<html xmlns=\"http://www.w3.org/1999/xhtml\" dir=\"ltr\" lang=\"en\">";
	private static String CURRENT_OPERATOR_NAME_READ_FROM_RAPIDWIKI;
	private static String ERROR_TEXT_FOR_JPANEL;
	
	private static final String RESOURCE_SUB_DIR = "com/rapidminer/resources";

	private static HashMap<OperatorDescription, String> OPERATOR_CACHE_MAP = new HashMap<OperatorDescription, String>();

	static {
		// TODO: Transfer this to resource
		ERROR_TEXT_FOR_JPANEL = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
				+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" dir=\"ltr\" lang=\"en\" xml:lang=\"en\">"
				+ "<head>"
				+ "<table cellpadding=0 cellspacing=0>"
				+ "<tr><td>"
				+ "<img src=\"" + SwingTools.getIconPath("48/bug_yellow_error.png") + "\"/>"
				+ "</td>"
				+ "<td width=\"5\">"
				+ "</td>"
				+ "<td>"
				+ "Error by reading selected operator from RapidWiki."
				+ "</td></tr>"
				+ "</table>"
				+ "</head>" + "</html>";
	}

	
	/**
	 * This is the method for loading an operator's documentation within the program.
	 */
	public static String loadOperatorDocumentation(final boolean online, final boolean activateCache, final OperatorDescription dirtyOpDesc) {
		String toShowText;
		OperatorDescription opDesc = dirtyOpDesc;
		// if (dirtyOpDesc == null && operatorDocViewer.getDisplayedOperator()
		// != null) {
		// opDesc =
		// operatorDocViewer.getDisplayedOperator().getOperatorDescription();
		// } else {
		// opDesc = dirtyOpDesc;
		// }
		if (opDesc == null) {
			// TODO: Eliminate this case
			toShowText = ERROR_TEXT_FOR_JPANEL;
		} else {
			if (activateCache && OPERATOR_CACHE_MAP.containsKey(opDesc)) {
				return OPERATOR_CACHE_MAP.get(opDesc);
			} else {
				try {
					if (online) {
						toShowText = loadSelectedOperatorDocuFromWiki(opDesc);
					} else {
						toShowText = loadSelectedOperatorDocuLocally(opDesc);
					}
				} catch (Exception e) {
					SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", e, true, e.getMessage());
					toShowText = ERROR_TEXT_FOR_JPANEL;
				}
				if (activateCache && StringUtils.isNotBlank(toShowText) && StringUtils.isNotEmpty(toShowText)) {
					OPERATOR_CACHE_MAP.put(opDesc, toShowText);
				}
			}
		}
		return toShowText;
	}

	public static void clearOperatorCache() {
		OPERATOR_CACHE_MAP.clear();
	}

	public static boolean hasCache(OperatorDescription opDesc) {
		return OPERATOR_CACHE_MAP.containsKey(opDesc);
	}
	
	/**
	 * 
	 * @param HTMLString
	 * @param operatorIconPath
	 * @return
	 */
	@Deprecated
	private static String customizeHTMLStringDirty(String HTMLString, String operatorIconPath) {
		HTMLString = HTMLString
				.replaceFirst("\\<[^\\>]*>",
						"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");

		// customize operator-name
		HTMLString = HTMLString.replaceFirst(CORRECT_HTML_STRING_DIRTY,
				"<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" dir=\"ltr\">" + "<head>" + "<table cellpadding=0 cellspacing=0>"
						+ "<tr><td>" + "<img src=\"" + operatorIconPath + "\" /></td>" + "<td width=\"5\">" + "</td>" + "<td>"
						+ "<h2 class=\"firstHeading\" id=\"firstHeading\">" + CURRENT_OPERATOR_NAME_READ_FROM_RAPIDWIKI + "</h2>"
						+ "</td></tr>" + "</table>" + "<hr noshade=\"true\">" + "</head>");

		// customize all headlines
		HTMLString = HTMLString.replaceAll("<h2>", "<h4>");
		HTMLString = HTMLString.replaceAll("</h2>", "</h4>");

		// replace all painted circles by icons
		HTMLString = HTMLString.replaceAll("<ul>", "<ul class=\"ports\">");

		// removing all <div class="visualClear"/>
		HTMLString = HTMLString.replaceAll("<div class=\"visualClear\"/>", "");

		// regex: \[theTag\][\w\s]*\[/theTag\]
		Pattern pattern = Pattern.compile("\\</div\\>[\\s]*\\<h4\\>|" + "\\</p\\>[\\s]*\\<h4\\>|" + "\\</h4\\>[\\s]*\\<h4\\>|"
				+ "\\</ul\\>[\\s]*\\<h4\\>|" + "\\</h4\\>[\\s]*\\</div\\>");
		Matcher matcher = pattern.matcher(HTMLString);
		while (matcher.find()) {
			String match = matcher.group();
			String replaceString = StringUtils.EMPTY;
			if (match.startsWith("</div")) {
				replaceString = "</div><br/><h4>";
			} else if (match.startsWith("</p")) {
				replaceString = "</p><br/><h4>";
			} else if (match.startsWith("</h4") && !match.contains("div")) {
				replaceString = "</h4><br/><h4>";
			} else if (match.startsWith("</ul")) {
				replaceString = "</ul><br/><h4>";
			} else if (match.startsWith("</h4") && match.contains("div")) {
				replaceString = "</h4><br/><div>";
			}
			HTMLString = HTMLString.replace(match, replaceString);
		}

		// replace <pre...> with <table...> because pre is not supported
		HTMLString = HTMLString.replaceAll("<pre", "<table class=pre border=0 bordercolor=black style=border-style:dashed;");
		// width=\"100%\"
		// border-collapse:separate;
		HTMLString = HTMLString.replaceAll("</pre", "</table");

		return HTMLString;
	}

	/**
	 * Checks the network connection.
	 * 
	 * @return <tt>true</tt> If the host is connected to network; <br>
	 *         <tt>false</tt> If the host is not connected to network.
	 */
	public static boolean hostHasNetworkConnection() {
		URL url = null;
		try {
			url = new URL(HOST_NAME);
		} catch (MalformedURLException e) {
			logger.error(e.getMessage());
		}
		if (url != null) {
			try {
				URLConnection urlConnection = url.openConnection();
				urlConnection.connect();
				InputStream is = urlConnection.getInputStream();
				if (is != null) {
					is.close();
					return true;
				}
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
		return false;
	}

	/**
	 * This method imports all operator documentations and images of operators
	 * from the given namespace from the wiki to the given resource directory.
	 * 
	 * @throws TransformerFactoryConfigurationError
	 * @throws Exception
	 */
	public static void importOperatorsAndImagesFromWiki(File resourceDir, String targetNamespace)
			throws TransformerFactoryConfigurationError, Exception {
		if (hostHasNetworkConnection()) {
			// create documentation directory in resources, if not exists
			File documentationDir = new File(resourceDir, RESOURCE_SUB_DIR);
			if (!documentationDir.exists()) {
				documentationDir.mkdir();
			}

			// create namespace directory
			File namespaceDir = new File(documentationDir, targetNamespace);
			if (!namespaceDir.exists()) {
				namespaceDir.mkdir();
			}

			// create images directories, if not exists
			File imageDirectory = new File(namespaceDir, IMAGE_DIR_NAME);
			if (!imageDirectory.exists()) {
				imageDirectory.mkdir();
			}

			logger.info("Saving operators...");
			RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.UI);
			OperatorService.init();
			for (String operatorName : OperatorService.getOperatorNames()) {
				Operator operator = OperatorService.createOperator(operatorName);
				OperatorDescription opDesc = operator.getOperatorDescription();
				if (!opDesc.isDeprecated() && (opDesc.getProvider() == null && targetNamespace.equals("core")) || opDesc.getProvider().getPrefix().equals(targetNamespace)) {
					String operatorWikiName = opDesc.getName().replaceAll(" ", "_");
					Document operatorDocument = parseDocumentForOperator(operatorWikiName, opDesc);
					if (operatorDocument != null) {
						String operatorKey = opDesc.getKey();

						// parse and save images for current operator
						operatorDocument = parseAndSaveImagesForOperator(operatorDocument, operatorName, imageDirectory);

						// save operator
						saveOperator(namespaceDir, operatorKey + ".html", calculateHTMLStringForOperator(operatorDocument, opDesc));
					}
				}

			}
		}
	}

	/**
	 * 
	 * @param documentOperator
	 * @param opDesc
	 * @return
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	private static String calculateHTMLStringForOperator(Document documentOperator, OperatorDescription opDesc)
			throws TransformerFactoryConfigurationError, TransformerException {
		// writing html back to string
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		// initialize StreamResult with File object to save to file
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(documentOperator);
		transformer.transform(source, result);

		String htmlString = result.getWriter().toString();
		htmlString = customizeHTMLStringDirty(htmlString, SwingTools.getIconPath("24/" + opDesc.getIconName()));
		return htmlString;
	}

	/**
	 * 
	 * @param documentOperator
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private static Document parseAndSaveImagesForOperator(Document documentOperator, String operatorName, File imageDirectory)
			throws ParserConfigurationException, SAXException, IOException {
		NodeList imgList = documentOperator.getElementsByTagName("img");
		if (imgList != null) {
			for (int k = 0; k < imgList.getLength(); k++) {
				Node img = imgList.item(k);
				Element imgElement = (Element) img;
				if (imgElement.getAttribute("class").equals("thumbimage")) {
					String srcString = imgElement.getAttribute("src");
					String[] splitSrc = srcString.split("/");
					String imageFileName = operatorName + "-" + splitSrc[splitSrc.length - 1];
					if (StringUtils.isNotEmpty(srcString) && StringUtils.isNotBlank(srcString)) {
						URL url = new URL(srcString);
						if (url != null) {
							try {
								ImageIO.write(ImageIO.read(url), IMAGE_FORMAT, new File(imageDirectory, imageFileName));
							} catch (MalformedURLException e) {
								logger.error(e.getMessage());
							} catch (IOException e) {
								logger.error(e.getMessage());
							}
							imgElement.removeAttribute("src");


							imgElement.setAttribute("src", "images/" + imageFileName);
						}
					}
				}
			}
		}
		return documentOperator;
	}

	/**
	 * 
	 * @param operatorDir
	 * @param operatorName
	 * @param operatorContent
	 */
	private static void saveOperator(File operatorDir, String operatorName, String operatorContent) {
		String temp = StringUtils.EMPTY;
		BufferedReader br = null;
		File targetFile = new File(operatorDir, operatorName);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(targetFile));
			br = new BufferedReader(new StringReader(operatorContent));
			temp = br.readLine();
			while (temp != null) {
				bw.write(temp);
				bw.newLine();
				temp = br.readLine();
			}
			bw.close();
			br.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * 
	 * @param operatorWikiName
	 * @param opDesc
	 * @return The parsed <tt>Document</tt> (not finally parsed) of the selected
	 *         operator.
	 * @throws MalformedURLException
	 * @throws ParserConfigurationException
	 */
	private static Document parseDocumentForOperator(String operatorWikiName, OperatorDescription opDesc) throws MalformedURLException,
			ParserConfigurationException {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setIgnoringComments(true);
		builderFactory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
		documentBuilder.setEntityResolver(new XHTMLEntityResolver());

		Document document = null;
		URL url = new URL(WIKI_PREFIX_FOR_OPERATORS + operatorWikiName);
		if (url != null) {
			try {
				document = documentBuilder.parse(url.openStream());
			} catch (IOException e) {
				logger.error("Folgende URL (parseDocumentForOperator()) konnte nicht geöffnet werden: " + e.getMessage());
			} catch (SAXException e) {
				logger.error("Folgende SAXParseException (parseDocumentForOperator()) ist aufgetreten: " + e.getMessage());
			}

			int i = 0;

			if (document != null) {
				Element contentElement = document.getElementById("content");

				// removing content element from document
				if (contentElement != null) {
					contentElement.getParentNode().removeChild(contentElement);
				}

				// removing everything from body
				NodeList bodies = document.getElementsByTagName("body");
				for (int k = 0; k < bodies.getLength(); k++) {
					Node body = bodies.item(k);
					while (body.hasChildNodes()) {
						body.removeChild(body.getFirstChild());
					}

					// read content element to body
					if (k == 0) {
						body.appendChild(contentElement);
					}
				}

				// removing everything from head
				NodeList heads = document.getElementsByTagName("head");
				for (int k = 0; k < heads.getLength(); k++) {
					Node head = heads.item(k);
					while (head.hasChildNodes()) {
						head.removeChild(head.getFirstChild());
					}
				}
				// removing...<head/> from document
				if (heads != null) {
					while (i < heads.getLength()) {
						Node head = heads.item(i);
						head.getParentNode().removeChild(head);
					}
				}

				// removing jump-to-nav element from document
				Element jumpToNavElement = document.getElementById("jump-to-nav");
				if (jumpToNavElement != null) {
					jumpToNavElement.getParentNode().removeChild(jumpToNavElement);
				}

				// removing mw-normal-catlinks element from document
				Element mwNormalCatlinksElement = document.getElementById("mw-normal-catlinks");
				if (mwNormalCatlinksElement != null) {
					mwNormalCatlinksElement.getParentNode().removeChild(mwNormalCatlinksElement);
				}

				// removing complete link navigation
				Element tocElement = document.getElementById("toc");
				if (tocElement != null) {
					tocElement.getParentNode().removeChild(tocElement);
				}

				// removing everything from class printfooter
				NodeList nodeListDiv = document.getElementsByTagName("div");
				for (int k = 0; k < nodeListDiv.getLength(); k++) {
					Element div = (Element) nodeListDiv.item(k);
					if (div.getAttribute("class").equals("printfooter")) {
						div.getParentNode().removeChild(div);
					}
				}

				// removing everything from class editsection
				NodeList spanList = document.getElementsByTagName("span");
				for (int k = 0; k < spanList.getLength(); k++) {
					Element span = (Element) spanList.item(k);
					if (span.getAttribute("class").equals("editsection")) {
						span.getParentNode().removeChild(span);
					}
				}

				// Synopsis Header
				boolean doIt = true;
				NodeList pList = document.getElementsByTagName("p");
				for (int k = 0; k < pList.getLength(); k++) {

					if (doIt) {
						Node p = pList.item(k);
						NodeList pChildList = p.getChildNodes();

						for (int j = 0; j < pChildList.getLength(); j++) {

							Node pChild = pChildList.item(j);
							if (pChild.getNodeType() == Node.TEXT_NODE && pChild.getNodeValue() != null
									&& StringUtils.isNotBlank(pChild.getNodeValue()) && StringUtils.isNotEmpty(pChild.getNodeValue())) {

								String pChildString = pChild.getNodeValue();
								Element newPWithoutSpaces = document.createElement("p");
								newPWithoutSpaces.setTextContent(pChildString);

								Node synopsis = document.createTextNode("Synopsis");

								Element span = document.createElement("span");
								span.setAttribute("class", "mw-headline");
								span.setAttribute("id", "Synopsis");
								span.appendChild(synopsis);

								Element h2 = document.createElement("h2");
								h2.appendChild(span);

								Element div = document.createElement("div");
								div.setAttribute("id", "synopsis");
								div.appendChild(h2);
								div.appendChild(newPWithoutSpaces);

								Node pChildParentParent = pChild.getParentNode().getParentNode();
								Node pChildParent = pChild.getParentNode();

								pChildParentParent.replaceChild(div, pChildParent);
								doIt = false;
								break;
							}
						}
					} else {
						break;
					}
				}

				// removing all <br...>-Tags
				NodeList brList = document.getElementsByTagName("br");

				while (i < brList.getLength()) {
					Node br = brList.item(i);
					Node parentBrNode = br.getParentNode();
					parentBrNode.removeChild(br);
				}

				// removing everything from script
				NodeList scriptList = document.getElementsByTagName("script");
				while (i < scriptList.getLength()) {
					Node scriptNode = scriptList.item(i);
					Node parentNode = scriptNode.getParentNode();
					parentNode.removeChild(scriptNode);
				}

				// removing all empty <p...>-Tags
				NodeList pList2 = document.getElementsByTagName("p");
				int ccc = 0;
				while (ccc < pList2.getLength()) {
					Node p = pList2.item(ccc);
					NodeList pChilds = p.getChildNodes();

					int kk = 0;

					while (kk < pChilds.getLength()) {
						Node pChild = pChilds.item(kk);
						if (pChild.getNodeType() == Node.TEXT_NODE) {
							String pNodeValue = pChild.getNodeValue();
							if (pNodeValue == null || StringUtils.isBlank(pNodeValue) || StringUtils.isEmpty(pNodeValue)) {
								kk++;
							} else {
								ccc++;
								break;
							}
						} else {
							ccc++;
							break;
						}
						if (kk == pChilds.getLength()) {
							Node parentBrNode = p.getParentNode();
							parentBrNode.removeChild(p);
						}
					}
				}

				// removing firstHeading element from document
				Element firstHeadingElement = document.getElementById("firstHeading");
				if (firstHeadingElement != null) {
					CURRENT_OPERATOR_NAME_READ_FROM_RAPIDWIKI = firstHeadingElement.getFirstChild().getNodeValue();
					firstHeadingElement.getParentNode().removeChild(firstHeadingElement);
				}

				// removing sitesub element from document
				Element siteSubElement = document.getElementById("siteSub");
				if (siteSubElement != null) {
					siteSubElement.getParentNode().removeChild(siteSubElement);
				}

				// removing contentSub element from document
				Element contentSubElement = document.getElementById("contentSub");
				if (contentSubElement != null) {
					contentSubElement.getParentNode().removeChild(contentSubElement);
				}

				// removing catlinks element from document
				Element catlinksElement = document.getElementById("catlinks");
				if (catlinksElement != null) {
					catlinksElement.getParentNode().removeChild(catlinksElement);
				}

				// removing <a...> element from document, if they are empty
				NodeList aList = document.getElementsByTagName("a");
				if (aList != null) {
					int k = 0;
					while (k < aList.getLength()) {
						Node a = aList.item(k);
						Element aElement = (Element) a;
						if (aElement.getAttribute("class").equals("internal")) {
							a.getParentNode().removeChild(a);
						} else {
							Node aChild = a.getFirstChild();
							if (aChild != null
									&& (aChild.getNodeValue() != null && aChild.getNodeType() == Node.TEXT_NODE
											&& StringUtils.isNotBlank(aChild.getNodeValue())
											&& StringUtils.isNotEmpty(aChild.getNodeValue()) || aChild.getNodeName() != null)) {
								Element aChildElement = null;
								if (aChild.getNodeName().startsWith("img")) {
									aChildElement = (Element) aChild;

									Element imgElement = document.createElement("img");
									imgElement.setAttribute("alt", aChildElement.getAttribute("alt"));
									imgElement.setAttribute("class", aChildElement.getAttribute("class"));
									imgElement.setAttribute("height", aChildElement.getAttribute("height"));
									imgElement.setAttribute("src", WIKI_PREFIX_FOR_IMAGES + aChildElement.getAttribute("src"));
									imgElement.setAttribute("width", aChildElement.getAttribute("width"));
									imgElement.setAttribute("border", "1");

									Node aParent = a.getParentNode();
									aParent.replaceChild(imgElement, a);
								} else {
									k++;
								}
							} else {
								a.getParentNode().removeChild(a);
							}
						}
					}
				}

			}
		}
		return document;
	}

	/**
	 * This method loads the documentation of the operator referenced by the operator description from
	 * the local resources if present. Otherwise an exception is thrown.
	 */
	private static String loadSelectedOperatorDocuLocally(OperatorDescription opDesc) throws UnsupportedEncodingException,
			ParserConfigurationException, URISyntaxException, IOException {
		String namespace = opDesc.getProviderNamespace();
		String documentationResource = "/" + RESOURCE_SUB_DIR + "/" + namespace + "/" + opDesc.getKey() + ".html";
		BufferedReader input = new BufferedReader(new InputStreamReader(OperatorDocImporter.class.getResourceAsStream(documentationResource)));
		try {
			String contents = Tools.readOutput(input);
			return contents;
		} 
		finally {
			input.close();
		}
		
	}

	/**
	 * This loads the documentation of the operator referenced by the operator description 
	 * from the Wiki in the internet.
	 */
	private static String loadSelectedOperatorDocuFromWiki(OperatorDescription opDesc) throws IOException, ParserConfigurationException,
			OperatorCreationException, TransformerException {
		String operatorWikiName = StringUtils.EMPTY;
		if (!opDesc.isDeprecated()) {
			operatorWikiName = opDesc.getName().replace(" ", "_");
		
			Document documentOperator = parseDocumentForOperator(operatorWikiName, opDesc);
		
			if (documentOperator != null) {
				// writing html back to string
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		
				// initialize StreamResult with File object to save to file
				StreamResult result = new StreamResult(new StringWriter());
				DOMSource source = new DOMSource(documentOperator);
				transformer.transform(source, result);
		
				String HTMLString = result.getWriter().toString();
				HTMLString = customizeHTMLStringDirty(HTMLString, SwingTools.getIconPath("24/" + opDesc.getIconName()));
		
				return HTMLString;
			}
		}
		return null;
	}
	public static void main(String[] args) throws TransformerFactoryConfigurationError, Exception {
		importOperatorsAndImagesFromWiki(new File("C:/Users/miguel/workspace/RapidMiner_Vega/resources"), "core");
	}
}