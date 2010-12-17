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
package com.rapidminer.tools.jdbc.connection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Key;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapidminer.io.Base64;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.cipher.CipherException;
import com.rapidminer.tools.cipher.CipherTools;
import com.rapidminer.tools.cipher.KeyGeneratorTool;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.DatabaseService;

/**
 * The central service for registering DatabaseConnections. They are used for
 * connection selection on all database related operators as well as for the import
 * wizards.
 * 
 * @author Tobias Malbrecht, Sebastian Land
 */
public class DatabaseConnectionService {
	
	public static final String PROPERTY_CONNECTIONS_FILE = "connections";
	
	private static List<FieldConnectionEntry> connections = new LinkedList<FieldConnectionEntry>();
	
	private static DatabaseHandler handler = null;

	public static void init() {
		File connectionsFile = getConnectionsFile(); 
		if (!connectionsFile.exists()) {
			try {
				connectionsFile.createNewFile();
			} catch (IOException ex) {
				// do nothing
			}
		} else {
			connections = readConnectionEntries(connectionsFile);
		}
	}
	
	private static File getConnectionsFile() {
		return ParameterService.getUserConfigFile(PROPERTY_CONNECTIONS_FILE);
	}
	
	public static Collection<FieldConnectionEntry> getConnectionEntries() {
		return connections;
	}
	
	public static ConnectionEntry getConnectionEntry(String name) {
		for (ConnectionEntry entry : connections) {
			if (entry.getName().equals(name)) {
				return entry;
			}
		}
		return null;
	}

	public static void addConnectionEntry(FieldConnectionEntry entry) {
		connections.add(entry);
		Collections.sort(connections, ConnectionEntry.COMPARATOR);
		writeConnectionEntries(connections);
	}

	public static void deleteConnectionEntry(ConnectionEntry entry) {
		connections.remove(entry);
		if (entry != null) {
			writeConnectionEntries(connections);
		}
	}
	

	public static void setConnectionEntries(List<FieldConnectionEntry> entries) {
		connections = entries;
		Collections.sort(connections, ConnectionEntry.COMPARATOR);
	}


//	public static void renameConnectionEntry(ConnectionEntry entry, String name) {
//		if (entry != null) {
//			entry.setName(name);
//			writeConnectionEntries(connections, connectionsFile);		
//		}
//	}
	
	public static List<FieldConnectionEntry> readConnectionEntries(File connectionEntriesFile) {
		LinkedList<FieldConnectionEntry> connectionEntries = new LinkedList<FieldConnectionEntry>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(connectionEntriesFile));
			String line = in.readLine();
			if (line != null) {
				int numberOfEntries = Integer.parseInt(line);
				for (int i = 0; i < numberOfEntries; i++) {
					String name = in.readLine();
					String system = in.readLine();
					String host = in.readLine();
					String port = in.readLine();
					String database = in.readLine();
					String user = in.readLine();
					String password = CipherTools.decrypt(in.readLine());
					if (name != null && system != null) {
						connectionEntries.add(new FieldConnectionEntry(name, DatabaseService.getJDBCProperties(system), host, port, database, user, password.toCharArray()));
					}
				}
			}
			in.close();
			Collections.sort(connectionEntries, ConnectionEntry.COMPARATOR);
		} catch (Exception e) {
			connectionEntries.clear();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// should not happen
				}
			}
		}
		return connectionEntries;
	}
	
	
	public static void writeConnectionEntries(Collection<FieldConnectionEntry> connectionEntries) {
		File connectionEntriesFile = getConnectionsFile();
		PrintWriter out;
		
		try {
			out = new PrintWriter(new FileWriter(connectionEntriesFile));
			writeConnectionEntries(connectionEntries, out);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Failed to write database connections file: "+e, e);
		}		
	}
	
	public static void writeConnectionEntries(Collection<FieldConnectionEntry> connectionEntries, PrintWriter out) {	
		try {
			// searching number of not dynamic entries to store it's number
			int numberOfEntries = 0;
			for (FieldConnectionEntry entry : connectionEntries) {
				if (!entry.isDynamic())
					numberOfEntries++;
			}
			out.println(numberOfEntries);
			
			// outputting each single non dynamic entry
			for (FieldConnectionEntry entry : connectionEntries) {
				if (!entry.isDynamic()) {
					out.println(entry.getName());
					out.println(entry.getProperties().getName());
					out.println(entry.getHost());
					out.println(entry.getPort());
					out.println(entry.getDatabase());
					out.println(entry.getUser());
					String encrypted;
					try {					
						encrypted = CipherTools.encrypt(new String(entry.getPassword()));
					} catch (CipherException e) {
						LogService.getRoot().log(Level.WARNING, "Failed to write database connections file: "+e, e);
						encrypted = null;
					}
					out.println(encrypted);
				}
			}
			out.close();
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	/**
	 * @param replacementForLocalhost The hostname "localhost" will be replaced by this string. Useful if 
	 *   this is exported to another machine where "localhost" has a different meaning. null=don't replace anything.
	 * 
	 */
	public static Document toXML(Collection<FieldConnectionEntry> connectionEntries, Key key, String replacementForLocalhost) throws ParserConfigurationException, DOMException, CipherException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("jdbc-entries");
		String base64key = Base64.encodeBytes(key.getEncoded());
		root.setAttribute("key", base64key);
		doc.appendChild(root);
		for (FieldConnectionEntry entry : connectionEntries) {
			root.appendChild(entry.toXML(doc, key, replacementForLocalhost));
		}
		return doc;
	}
	
	public static Collection<FieldConnectionEntry> parseEntries(Element entries) throws XMLException, CipherException, IOException {
		if (!entries.getTagName().equals("jdbc-entries")) {
			throw new XMLException("Outer tag must be <jdbc-entries>");
		}
		String base64Key = entries.getAttribute("key");
		if (base64Key == null) {
			throw new XMLException("Cipher key attribute missing.");
		}
		Key key = KeyGeneratorTool.makeKey(Base64.decode(base64Key));
		Collection<FieldConnectionEntry> result = new LinkedList<FieldConnectionEntry>();
		NodeList children = entries.getElementsByTagName(FieldConnectionEntry.XML_TAG_NAME);
		for (int i = 0; i < children.getLength(); i++) {
			result.add(new FieldConnectionEntry((Element) children.item(i), key));
		}
		return result;
	}

	
	public static boolean testConnection(ConnectionEntry entry) throws SQLException {
    	if (entry != null) {
    		if (handler != null) {
    			handler.disconnect();
    		}
            handler = DatabaseHandler.getConnectedDatabaseHandler(entry);
            if (handler != null) {
            	handler.disconnect();
            }
            return true;
    	}
    	return false;
	}
}
