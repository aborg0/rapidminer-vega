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
package com.rapidminer.tools.jdbc.connection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.filechooser.FileSystemView;

import com.rapidminer.tools.cipher.CipherTools;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.DatabaseService;

/**
 * @author Tobias Malbrecht
 */
public class DatabaseConnectionService {

	public static final String PROPERTY_CONNECTIONS_DIR = "connections.dir";
	
	public static final String PROPERTY_CONNECTIONS_FILE = ".connections";
	
	private static FileSystemView fileSystemView = FileSystemView.getFileSystemView();

	private static File connectionsFile;

	private static List<ConnectionEntry> connections = new LinkedList<ConnectionEntry>();
	
	private static DatabaseHandler handler = null;

	public static void init() {
		File tempFile = fileSystemView.getHomeDirectory();
		tempFile = tempFile.getAbsoluteFile();
		File parentTempFile = tempFile.getParentFile();

		String connectionsDirProperty = System.getProperty(PROPERTY_CONNECTIONS_DIR);
		if ((connectionsDirProperty != null) && (connectionsDirProperty.length() > 0)) {
			File applicationSpecifiedDir = new File(connectionsDirProperty);
			if (applicationSpecifiedDir.exists()) {
				parentTempFile = applicationSpecifiedDir;
			}
		}

		try {
			parentTempFile = parentTempFile.getCanonicalFile();
		} catch (Exception exp) {
		}

		if ((parentTempFile != null) && parentTempFile.exists() && fileSystemView.isTraversable(parentTempFile).booleanValue()) {
			connectionsFile = new File(parentTempFile, PROPERTY_CONNECTIONS_FILE);

			try {
				connectionsFile.createNewFile();
			} catch (IOException ex2) {
			}

			if (!connectionsFile.exists()) {
				connectionsFile.delete();
				connectionsFile = new File(tempFile, PROPERTY_CONNECTIONS_FILE);
			}
		} else {
			connectionsFile = new File(tempFile, PROPERTY_CONNECTIONS_FILE);
		}

		
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
	
	public static Collection<ConnectionEntry> getConnectionEntries() {
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

	public static void addConnectionEntry(ConnectionEntry entry) {
		connections.add(entry);
		Collections.sort(connections, ConnectionEntry.COMPARATOR);
		writeConnectionEntries(connections, connectionsFile);
	}

	public static void deleteConnectionEntry(ConnectionEntry entry) {
		connections.remove(entry);
		if (entry != null) {
			writeConnectionEntries(connections, connectionsFile);
		}
	}

//	public static void renameConnectionEntry(ConnectionEntry entry, String name) {
//		if (entry != null) {
//			entry.setName(name);
//			writeConnectionEntries(connections, connectionsFile);		
//		}
//	}
	
	public static List<ConnectionEntry> readConnectionEntries(File connectionEntriesFile) {
		LinkedList<ConnectionEntry> connectionEntries = new LinkedList<ConnectionEntry>();
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
	
	public static void writeConnectionEntries(Collection<ConnectionEntry> connectionEntries, File connectionEntriesFile) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(connectionEntriesFile));
			out.println(connectionEntries.size());
			for (ConnectionEntry connectionEntry : connectionEntries) {
				FieldConnectionEntry entry = (FieldConnectionEntry) connectionEntry;
				out.println(entry.getName());
				out.println(entry.getProperties().getName());
				out.println(entry.getHost());
				out.println(entry.getPort());
				out.println(entry.getDatabase());
				out.println(entry.getUser());
				out.println(CipherTools.encrypt(new String(entry.getPassword())));
			}
			out.close();
		} catch (Exception e) {
			// do nothing
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	public static DatabaseHandler connect(ConnectionEntry entry) throws SQLException {
		DatabaseHandler handler = new DatabaseHandler(entry.getURL(), entry.getProperties());
		handler.connect(entry.getUser(), new String(entry.getPassword()), true);
		return handler;
	}
	
	public static boolean testConnection(ConnectionEntry entry) throws SQLException {
    	if (entry != null) {
    		if (handler != null) {
    			handler.disconnect();
    		}
            handler = connect(entry);
            if (handler != null) {
            	handler.disconnect();
            }
            return true;
    	}
    	return false;
	}
}
