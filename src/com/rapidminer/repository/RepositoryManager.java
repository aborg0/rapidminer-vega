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
package com.rapidminer.repository;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.gui.RepositoryDialog;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.repository.remote.RemoteRepository;
import com.rapidminer.repository.resource.ResourceRepository;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;

/** Keeps static references to registered repositories.
 *
 * @author Simon Fischer
 *
 */
public class RepositoryManager extends AbstractObservable<Repository> {

	private static final Logger LOGGER = Logger.getLogger(RepositoryManager.class.getName());

	private static RepositoryManager instance;
	private static final Object INSTANCE_LOCK = new Object();
	private static Repository sampleRepository;
	private static final Map<RepositoryAccessor,RepositoryManager> CACHED_MANAGERS = new HashMap<RepositoryAccessor,RepositoryManager>();
	private static final List<RepositoryFactory> FACTORIES = new LinkedList<RepositoryFactory>();

	private static final String PROPERTY_HOME_REPOSITORY_URL = "rapidminer.homerepository.url";
	private static final String PROPERTY_HOME_REPOSITORY_USER = "rapidminer.homerepository.user";
	
	private final List<Repository> repositories = new LinkedList<Repository>();
	
	public static RepositoryManager getInstance(RepositoryAccessor repositoryAccessor) {
		synchronized (INSTANCE_LOCK) {
			if (instance == null) {
				init();
			}
			if (repositoryAccessor != null) {
				RepositoryManager manager = CACHED_MANAGERS.get(repositoryAccessor);
				if (manager == null) {
					manager = new RepositoryManager(instance);
					for (RepositoryFactory factory : FACTORIES) {
						for (Repository repos : factory.createRepositoriesFor(repositoryAccessor)) {
							manager.repositories.add(repos);
						}
					}
					CACHED_MANAGERS.put(repositoryAccessor, manager);
				}
				return manager;
			}
		}
		return instance;
	}	

	private RepositoryManager(RepositoryManager cloned) {
		this.repositories.addAll(cloned.repositories); 
	}
	
	private RepositoryManager() {
		if (sampleRepository == null) {
			sampleRepository = new ResourceRepository("Samples", "/"+Tools.RESOURCE_PREFIX+"samples");
		}
		repositories.add(sampleRepository);
		
		final String homeUrl = System.getProperty(PROPERTY_HOME_REPOSITORY_URL);
		if (homeUrl != null) {
			try {
				RemoteRepository homeRepository = new RemoteRepository(new URL(homeUrl), "Home", System.getProperty(PROPERTY_HOME_REPOSITORY_USER), null);
				repositories.add(homeRepository);
				LogService.getRoot().config("Adding home repository "+homeUrl+".");
			} catch (MalformedURLException e) {
				LogService.getRoot().log(Level.WARNING, "Illegal repository URL "+homeUrl+": "+e, e);
			}
		}
		load();
	}

	public static void init() {
		synchronized (INSTANCE_LOCK) {
			instance = new RepositoryManager();
		}
	}

	public static void registerFactory(RepositoryFactory factory) {
		synchronized (INSTANCE_LOCK) {
			FACTORIES.add(factory);	
		}		
	}
	
	public void addRepository(Repository repository) {
		LOGGER.config("Adding repository "+repository.getName());
		repositories.add(repository);
		save();
		fireUpdate(repository);
	}

	public void removeRepository(Repository repository) {
		repositories.remove(repository);
		fireUpdate(null);
	}

	public List<Repository> getRepositories() {
		return Collections.unmodifiableList(repositories);
	}

	public Repository getRepository(String name) {
		for (Repository repos : repositories) {
			if (repos.getName().equals(name)) {
				return repos;
			}
		}
		LogService.getRoot().warning("Requested repository "+name + " does not exist.");
		return null;
	}

	public List<RemoteRepository> getRemoteRepositories() {
		List<RemoteRepository> result = new LinkedList<RemoteRepository>();
		for (Repository repos : getRepositories()) {
			if (repos instanceof RemoteRepository) {
				result.add((RemoteRepository)repos);
			}
		}
		return result;
	}

	private File getConfigFile() {
		return ParameterService.getUserConfigFile("repositories.xml");
	}

	public void load() {
		if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
			LOGGER.info("Cannot access file system in execution mode "+RapidMiner.getExecutionMode()+". Not loading repositories.");
			return;
		}
		File file = getConfigFile();
		if (file.exists()) {
			LOGGER.config("Loading repositories from "+file);
			try {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
				if (!doc.getDocumentElement().getTagName().equals("repositories")) {
					LOGGER.warning("Broken repositories file. Root element must be <reposities>.");
					return;
				}
				NodeList list = doc.getDocumentElement().getChildNodes();				
				for (int i = 0; i < list.getLength(); i++) {
					if (list.item(i) instanceof Element) {
						Element element = (Element)list.item(i);
						if ("localRepository".equals(element.getTagName())) {
							addRepository(LocalRepository.fromXML(element));
						} else if ("remoteRepository".equals(element.getTagName())) {
							addRepository(RemoteRepository.fromXML(element));
						} else {
							LOGGER.warning("Unknown tag: "+element.getTagName());			
						}
					}					
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Cannot read repository configuration file '"+file+"': "+e, e);
			}
		}
	}

	public void createRepositoryIfNoneIsDefined() {
		boolean empty = true;
		// check if we have at least one repository that is not pre-defined
		for (Repository repository : repositories) {
			if (!(repository instanceof ResourceRepository)) {
				empty = false;
				break;
			}
		}
		if (empty) {
			SwingTools.showMessageDialog("please_create_repository");
			RepositoryDialog.createNew();
		}
	}

	public void save() {
		if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
			LOGGER.config("Cannot access file system in execution mode "+RapidMiner.getExecutionMode()+". Not saving repositories.");
			return;
		}
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			LOGGER.log(Level.WARNING, "Cannot save repositories: "+e, e);
			return;			
		}
		Element root = doc.createElement("repositories");
		doc.appendChild(root);
		for (Repository repository : getRepositories()) {
			Element repositoryElement = repository.createXML(doc);
			if (repositoryElement != null) {
				root.appendChild(repositoryElement);
			}
		}
		try {
			XMLTools.stream(doc, getConfigFile(), null);
		} catch (XMLException e) {
			LOGGER.log(Level.WARNING, "Cannot save repositories: "+e, e);
		}
	}

	public IOObject store(IOObject ioobject, RepositoryLocation location, Operator callingOperator) throws RepositoryException {
		Entry entry = location.locateEntry();
		if (entry == null) {
			RepositoryLocation parentLocation = location.parent();
			if (parentLocation != null) {
				String childName = location.getName();
				Entry parentEntry = parentLocation.locateEntry();
				Folder parentFolder;				
				if (parentEntry != null) {
					if (parentEntry instanceof Folder) {
						parentFolder = (Folder)parentEntry;						
					} else {
						throw new RepositoryException("Parent '"+parentLocation+"' of '"+location+"' is not a folder.");
					}
				} else {
					parentFolder = parentLocation.createFoldersRecursively();
				}
				parentFolder.createIOObjectEntry(childName, ioobject, callingOperator, null);
				return ioobject;
			} else {
				throw new RepositoryException("Entry '"+location+"' does not exist.");
			}
		} else if (entry instanceof IOObjectEntry) {
			((IOObjectEntry) entry).storeData(ioobject, callingOperator, null);
			return ioobject;
		} else {
			throw new RepositoryException("Entry '"+location+"' is not a data entry, but "+entry.getType());
		}
	}
	
	public static void shutdown() {
		if (instance != null) {
			instance.save();
		}		
	}

	/** Copies an entry to a given destination folder. */
	public void copy(RepositoryLocation source, Folder destination, ProgressListener l) throws RepositoryException {
		Entry entry = source.locateEntry();
		if (entry == null) {
			throw new RepositoryException("No such entry: "+source);
		} else {
			String newName = source.getName();
			if (destination.containsEntry(newName)) {
				newName = "Copy of " + source.getName();
				int i = 1;
				while (destination.containsEntry(newName)) {
					newName = "Copy "+(i++)+" of "+source.getName();
				}
			}
			if (entry instanceof ProcessEntry) {
				l.setTotal(100);
				l.setCompleted(10);
				ProcessEntry pe = (ProcessEntry) entry;				
				String xml = pe.retrieveXML();
				l.setCompleted(50);
				destination.createProcessEntry(newName, xml);
				l.setCompleted(100);
				l.complete();
			} else if (entry instanceof IOObjectEntry) {
				IOObjectEntry iooe = (IOObjectEntry) entry;
				destination.createIOObjectEntry(newName, iooe.retrieveData(l), null, l);
			} else if (entry instanceof BlobEntry) {
				BlobEntry blob = (BlobEntry) entry;
				BlobEntry target = destination.createBlobEntry(newName);
				try {
					Tools.copyStreamSynchronously(blob.openInputStream(), target.openOutputStream(blob.getMimeType()), true);
				} catch (IOException e) {
					throw new RepositoryException(e);
				}
			} else {
				throw new RepositoryException("Cannot copy entry of type "+entry.getType());
			}
		}
	}
	
	/** Moves an entry to a given destination folder. */
	public void move(RepositoryLocation source, Folder destination, ProgressListener l) throws RepositoryException {
		Entry entry = source.locateEntry();
		if (entry == null) {
			throw new RepositoryException("No such entry: "+source);
		} else {
			if (destination.getLocation().getRepository() != source.getRepository()) {
				copy(source, destination, l);
				entry.delete();
			} else {
				String newName = source.getName();
				if (destination.containsEntry(newName)) {
					throw new RepositoryException("Destination contains element with name: "+newName);
				}
				l.setTotal(100);
				l.setCompleted(10);
				entry.move(destination);
				l.setCompleted(100);
				l.complete();
			}
		}
	}
	
	/** Looks up the entry with the given path in the given repository.
	 *  This method will return null when it finds a folder that blocks (has not yet loaded
	 *  all its data) AND failIfBlocks is true.
	 *  
	 *  This method can be used as a first approach to locate an entry and fall back
	 *  to a more expensive solution when this fails.
	 *  
	 */
	public Entry locate(Repository repository, String path, boolean failIfBlocks) throws RepositoryException {
		if (path.startsWith(""+RepositoryLocation.SEPARATOR)) {
			path = path.substring(1);
		}
		if (path.equals("")) {
			return repository;
		}
		String[] splitted = path.split(""+RepositoryLocation.SEPARATOR);
		Folder folder = repository;
		int index = 0;		
		while (true) {
			if (failIfBlocks && folder.willBlock()) {
				return null;
			}
			if (index == splitted.length-1) {
				List<Entry> all = new LinkedList<Entry>();
				all.addAll(folder.getSubfolders());
				all.addAll(folder.getDataEntries());
				for (Entry child : all) {
					if (child.getName().equals(splitted[index])) {
						return child;
					}
				}
				return null;
			} else {
				boolean found = false;
				for (Folder subfolder : folder.getSubfolders()) {
					if (subfolder.getName().equals(splitted[index])) {
						folder = subfolder;
						found = true;
						break;
					}
				}
				if (!found) {
					return null;	
				}
			}
			index++;
		}
	}

	public Repository getSampleRepository() {	
		return sampleRepository;
	}
}
