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
package com.rapidminer.repository.remote;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.event.EventListenerList;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapid_i.repository.wsimport.EntryResponse;
import com.rapid_i.repository.wsimport.RepositoryService;
import com.rapid_i.repository.wsimport.RepositoryServiceService;
import com.rapidminer.RapidMiner;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryConstants;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryListener;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.tools.GlobalAuthenticator;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;
/**
 * @author Simon Fischer
 */
public class RemoteRepository extends RemoteFolder implements Repository {

	public static enum EntryStreamType {
		METADATA, IOOBJECT, PROCESS, BLOB
	}

	public static enum RMContentType {		
		IOOBJECT("application/vnd.rapidminer.ioo"),
		METADATA("application/vnd.rapidminer.md"),
		PROCESS("application/vnd.rapidminer.rmp+xml");
		
		private String typeString;
		RMContentType(String typeString) {
			this.typeString = typeString;
		}
		public String getContentTypeString() {
			return typeString;
		}
	}
	
	private final URL baseUrl;
	private String alias;
	private final String username;
	private char[] password;
	private RepositoryService repositoryService;
	private final EventListenerList listeners = new EventListenerList();

	private static final Map<URL,WeakReference<RemoteRepository>> ALL_REPOSITORIES = new HashMap<URL,WeakReference<RemoteRepository>>();
	private static final Object MAP_LOCK = new Object(); 	

	private boolean offline = false; 
	
	static {
		GlobalAuthenticator.register(new GlobalAuthenticator.URLAuthenticator() {
			@Override
			public PasswordAuthentication getAuthentication(URL url) {			
				WeakReference<RemoteRepository> reposRef = ALL_REPOSITORIES.get(url);
				if (reposRef == null) {
					return null;
				}
				RemoteRepository repository = reposRef.get();
				if (repository != null) {
					return repository.getAuthentiaction();					
				} else {					
					return null;
				}				
			}

			@Override
			public String getName() {
				return "Repository authenticator.";
			}
		});
	}
	
	public RemoteRepository(URL baseUrl, String alias, String username, char[] password) {		
		super("/");
		setRepository(this);
		this.alias = alias;
		this.baseUrl = baseUrl;
		this.username = username;
		if ((password != null) && (password.length > 0)) {
			this.password = password;
		} else {
			this.password = null;
		}
		register(this);
	}
	
	private static void register(RemoteRepository remoteRepository) {
		synchronized (MAP_LOCK) {
			ALL_REPOSITORIES.put(remoteRepository.getWSDLUrl(), new WeakReference<RemoteRepository>(remoteRepository));
		}
	}

	private URL getWSDLUrl() {
		try {
			return new URL(baseUrl, "RepositoryServiceService?wsdl");
		} catch (MalformedURLException e) {
			// cannot happen
			LogService.getRoot().log(Level.WARNING, "Cannot create web service url: "+e, e);
			return null;
		}
	}

	@Override
	public void addRepositoryListener(RepositoryListener l) {
		listeners.add(RepositoryListener.class, l);
	}

	@Override
	public void removeRepositoryListener(RepositoryListener l) {
		listeners.remove(RepositoryListener.class, l);		
	}
	
	@Override
	public boolean rename(String newName) {
		this.alias = newName;
		fireEntryRenamed(this);
		return true;
	}
	
	protected void fireEntryRenamed(Entry entry) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			l.entryRenamed(entry);
		}
	}
	
	protected void fireEntryAdded(Entry newEntry, Folder parent) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			l.entryAdded(newEntry, parent);
		}
	}
	
	protected void fireEntryRemoved(Entry removedEntry, Folder parent, int index) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			l.entryRemoved(removedEntry, parent, index);
		}
	}

	protected void fireRefreshed(Folder folder) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			l.folderRefreshed(folder);
		}		
	}
	
	@Override
	public Entry locate(String string) throws RepositoryException {
		Entry firstTry = RepositoryManager.locate(this, string, true);
		if (firstTry != null) {
			return firstTry;
		}
		
		if (!string.startsWith("/")) {
			string = "/" + string;
		}
		
		EntryResponse response = getService().getEntry(string);
		if (response.getStatus() != RepositoryConstants.OK) {
			if (response.getStatus() == RepositoryConstants.NO_SUCH_ENTRY) {
				return null;
			}
			throw new RepositoryException(response.getErrorMessage());
		}
		if (response.getType().equals(Folder.TYPE_NAME)) {
			return new RemoteFolder(response, null, this);
		} else if (response.getType().equals(ProcessEntry.TYPE_NAME)) {
			return new RemoteProcessEntry(response, null, this);
		} else if (response.getType().equals(IOObjectEntry.TYPE_NAME)) {
			return new RemoteIOObjectEntry(response, null, this);
		} else if (response.getType().equals(BlobEntry.TYPE_NAME)) {
			return new RemoteBlobEntry(response, null, this);
		} else {
			throw new RepositoryException("Unknown entry type: " + response.getType());
		}
	}

	@Override
	public String getName() {
		return alias; 
	}
	
	@Override
	public String getState() {
		return (offline ? "offline" : (repositoryService != null ? "connected" : "disconnected"));
	}
	
	@Override
	public String toString() {
		return "<html>" + alias + "<br/><small style=\"color:gray\">(" + baseUrl + ")</small></html>";
	}
	
	private PasswordAuthentication getAuthentiaction() {
		if (this.password == null) {
			LogService.getRoot().info("Authentication requested for URL: "+baseUrl);
			String passwdString = RapidMiner.getInputHandler().inputPassword(I18N.getMessage(I18N.getGUIBundle(),
					"gui.label.repositorydialog.askpassword.message", username, baseUrl.toString()));
			if (passwdString == null) {
				return null;
			}
			this.password = passwdString.toCharArray();
			return new PasswordAuthentication(username, passwdString.toCharArray());
		} else {
			return new PasswordAuthentication(username, this.password);
		}
	}

	public RepositoryService getService() throws RepositoryException {
		if (offline) {
			throw new RepositoryException("Repository "+getName()+" is offline. Connect first.");
		}
		if (repositoryService == null){
			try {
				RepositoryServiceService serviceService = new RepositoryServiceService(getWSDLUrl(), 
						new QName("http://service.web.rapidrepository.com/", "RepositoryServiceService"));
				repositoryService = serviceService.getRepositoryServicePort();
			} catch (Exception e) {
				offline = true;
				password = null;
				throw new RepositoryException("Cannot connect to "+baseUrl+": "+e, e);				
			}
		}
		return repositoryService;
	}
	
	@Override
	public String getDescription() {
		return "Remote repository at "+baseUrl;
	}

	@Override
	public void refresh() throws RepositoryException {
		offline = false;
		super.refresh();
	}
		
	protected HttpURLConnection getHTTPConnection(String location, EntryStreamType type) throws IOException {		
		String split[] = location.split("/");
		StringBuilder encoded = new StringBuilder();
		encoded.append("resources");
		for (String fraction : split) {
			encoded.append('/');
			encoded.append(URLEncoder.encode(fraction, "UTF-8"));
		}
		if (type == EntryStreamType.METADATA) {
			encoded.append("?format=binmeta");
		}
		return (HttpURLConnection) new URL(baseUrl, encoded.toString()).openConnection();
	}
	
	@Override
	public Element createXML(Document doc) {
		Element repositoryElement = doc.createElement("remoteRepository");
		
		Element url = doc.createElement("url");
		url.appendChild(doc.createTextNode(this.baseUrl.toString()));
		repositoryElement.appendChild(url);
		
		Element alias = doc.createElement("alias");
		alias.appendChild(doc.createTextNode(this.alias));
		repositoryElement.appendChild(alias);
		
		Element user = doc.createElement("user");
		user.appendChild(doc.createTextNode(this.username));
		repositoryElement.appendChild(user);
		
		return repositoryElement;
	}
	
	public static RemoteRepository fromXML(Element element) throws XMLException {
		String url = XMLTools.getTagContents(element, "url", true);
		try {
			return new RemoteRepository(new URL(url),
					XMLTools.getTagContents(element, "alias", true),
					XMLTools.getTagContents(element, "user", true),
					null);
		} catch (MalformedURLException e) {
			throw new XMLException("Illegal url '"+url+"': "+e, e);
		}
	}
	

	@Override
	public void delete() {
		RepositoryManager.getInstance().removeRepository(this);
	}
}
