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
package com.rapidminer.repository.remote;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.event.EventListenerList;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapid_i.repository.wsimport.EntryResponse;
import com.rapid_i.repository.wsimport.ProcessService;
import com.rapid_i.repository.wsimport.ProcessService_Service;
import com.rapid_i.repository.wsimport.RepositoryService;
import com.rapid_i.repository.wsimport.RepositoryService_Service;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.actions.BrowseAction;
import com.rapidminer.gui.tools.SwingTools;
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

	private final URL baseUrl;
	private String alias;
	private final String username;
	private char[] password;
	private RepositoryService repositoryService;
	private ProcessService processService;
	private final EventListenerList listeners = new EventListenerList();

	private static final Map<URL,WeakReference<RemoteRepository>> ALL_REPOSITORIES = new HashMap<URL,WeakReference<RemoteRepository>>();
	private static final Object MAP_LOCK = new Object();

	private boolean offline = true; 
	
	static {
		GlobalAuthenticator.register(new GlobalAuthenticator.URLAuthenticator() {
			@Override
			public PasswordAuthentication getAuthentication(URL url) {			
				WeakReference<RemoteRepository> reposRef = null;// = ALL_REPOSITORIES.get(url);
				for (Map.Entry<URL, WeakReference<RemoteRepository>> entry : ALL_REPOSITORIES.entrySet()) {
					if (url.toString().startsWith(entry.getKey().toString())) {
						reposRef = entry.getValue();
						break;
					}
				}
				
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
			ALL_REPOSITORIES.put(remoteRepository.baseUrl, new WeakReference<RemoteRepository>(remoteRepository));
		}
	}

	private URL getRepositoryServiceWSDLUrl() {
		try {
			return new URL(baseUrl, "RepositoryService?wsdl");
		} catch (MalformedURLException e) {
			// cannot happen
			LogService.getRoot().log(Level.WARNING, "Cannot create web service url: "+e, e);
			return null;
		}
	}
	
	private URL getProcessServiceWSDLUrl() {
		try {
			return new URL(baseUrl, "ProcessService?wsdl");
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
	
	private Map<String,RemoteEntry> cachedEntries = new HashMap<String,RemoteEntry>();
	protected void register(RemoteEntry entry) {
		cachedEntries.put(entry.getPath(), entry);
	}
	
	@Override
	public Entry locate(String string) throws RepositoryException {
		Entry cached = cachedEntries.get(string);
		if (cached != null) {
			return cached;
		}
		Entry firstTry = RepositoryManager.getInstance(null).locate(this, string, true);
		if (firstTry != null) {
			return firstTry;
		}
		
		if (!string.startsWith("/")) {
			string = "/" + string;
		}
		
		EntryResponse response = getRepositoryService().getEntry(string);
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

	public RepositoryService getRepositoryService() throws RepositoryException {
//		if (offline) {
//			throw new RepositoryException("Repository "+getName()+" is offline. Connect first.");
//		}
		if (repositoryService == null){
			try {
				RepositoryService_Service serviceService = new RepositoryService_Service(getRepositoryServiceWSDLUrl(), 
						new QName("http://service.web.rapidrepository.com/", "RepositoryService"));
				repositoryService = serviceService.getRepositoryServicePort();
				offline = false;
			} catch (Exception e) {
				offline = true;
				password = null;
				repositoryService = null;
				throw new RepositoryException("Cannot connect to "+baseUrl+": "+e, e);				
			}
		}
		return repositoryService;
	}

	public ProcessService getProcessService() throws RepositoryException {
//		if (offline) {
//			throw new RepositoryException("Repository "+getName()+" is offline. Connect first.");
//		}
		if (processService == null){
			try {
				ProcessService_Service serviceService = new ProcessService_Service(getProcessServiceWSDLUrl(), 
						new QName("http://service.web.rapidrepository.com/", "ProcessService"));
				processService = serviceService.getProcessServicePort();
				offline = false;
			} catch (Exception e) {
				offline = true;
				password = null;
				processService = null;
				throw new RepositoryException("Cannot connect to "+baseUrl+": "+e, e);				
			}
		}
		return processService;
	}

	@Override
	public String getDescription() {
		return "Remote repository at "+baseUrl;
	}

	@Override
	public void refresh() throws RepositoryException {
		offline = false;
		cachedEntries.clear();
		super.refresh();
	}
		
	protected HttpURLConnection getHTTPConnection(String location, EntryStreamType type) throws IOException {		
		String split[] = location.split("/");
		StringBuilder encoded = new StringBuilder();
		encoded.append("resources");
		for (String fraction : split) {
			encoded.append('/');
			encoded.append(URLEncoder.encode(fraction, "UTF-8"));
			//encoded.append(fraction);
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
		RepositoryManager.getInstance(null).removeRepository(this);
	}

	public static List<RemoteRepository> getAll() {
		List<RemoteRepository> result = new LinkedList<RemoteRepository>();
		for (WeakReference<RemoteRepository> ref : ALL_REPOSITORIES.values()) {
			RemoteRepository rep = ref.get();
			if (ref != null) {
				result.add(rep);
			}
		}
		return result;
	}
	
	public boolean isConnected() {
		return !offline;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result + ((baseUrl == null) ? 0 : baseUrl.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RemoteRepository other = (RemoteRepository) obj;
		if (alias == null) {
			if (other.alias != null)
				return false;
		} else if (!alias.equals(other.alias))
			return false;
		if (baseUrl == null) {
			if (other.baseUrl != null)
				return false;
		} else if (!baseUrl.equals(other.baseUrl))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	public URI getURIForResource(String path) {
		try {
			return baseUrl.toURI().resolve("faces/browse.xhtml?location="+URLEncoder.encode(path, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);			
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	

	private URI getURIWebInterfaceURI() {
		try {
			return baseUrl.toURI().resolve("faces/index.xhtml");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public void browse(String location) {
		try {
			Desktop.getDesktop().browse(getURIForResource(location));
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("cannot_open_browser", e);
		}		
	}
	
	public void showLog(int id) {
		try {
			Desktop.getDesktop().browse(getProcessLogURI(id));
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("cannot_open_browser", e);
		}				
	}

	public URI getProcessLogURI(int id) {
		try {
			return baseUrl.toURI().resolve("processlog?id="+id);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}	

	@Override
	public Collection<Action> getCustomActions() {
		Collection<Action> actions = super.getCustomActions();
		actions.add(new BrowseAction("remoterepository.administer", getRepository().getURIWebInterfaceURI()));
		return actions;
	}
}
