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
package com.rapidminer.repository.local;

import java.io.File;
import java.util.logging.Level;

import javax.swing.event.EventListenerList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryListener;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;

/** A repository backed by the local file system. Each entry is backed by one or more files.
 * 
 * @author Simon Fischer
 *
 */
public class LocalRepository extends SimpleFolder implements Repository {

	private final EventListenerList listeners = new EventListenerList();

	private final File root;

	public LocalRepository(String name, File root) throws RepositoryException {
		super(name, null, null);		
		setRepository(this);
		this.root = root;
		try {
			mkdir();
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Failed to create repository directory: "+e, e);
		}
	}

	protected File getRoot() {
		return this.root;
	}	

	@Override
	public boolean rename(String newName) {
		setName(newName);
		fireEntryRenamed(this);
		return true;
	}

	@Override
	public File getFile() {
		return getRoot();
	}

	@Override
	public void addRepositoryListener(RepositoryListener l) {
		listeners.add(RepositoryListener.class, l);
	}

	@Override
	public void removeRepositoryListener(RepositoryListener l) {
		listeners.remove(RepositoryListener.class, l);		
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

	public void fireRefreshed(Folder folder) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			l.folderRefreshed(folder);
		}
	}


	protected void fireEntryRemoved(Entry removedEntry, Folder parent, int index) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			l.entryRemoved(removedEntry, parent, index);
		}
	}

	@Override
	public String getDescription() {
		return "This is a local repository stored on your local computer only."; 
	}

	@Override
	public Entry locate(String entry) throws RepositoryException {
		return RepositoryManager.getInstance(null).locate(this, entry, false);
	}

	@Override
	public RepositoryLocation getLocation() {
		try {
			return new RepositoryLocation(getName(), new String[0]);
		} catch (MalformedRepositoryLocationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getState() {
		return null;
	}

	@Override
	public Element createXML(Document doc) {
		Element repositoryElement = doc.createElement("localRepository");

		Element file = doc.createElement("file");
		file.appendChild(doc.createTextNode(this.root.getAbsolutePath()));
		repositoryElement.appendChild(file);

		Element name = doc.createElement("alias");
		name.appendChild(doc.createTextNode(this.getName()));
		repositoryElement.appendChild(name);

		return repositoryElement;
	}

	public static LocalRepository fromXML(Element element) throws XMLException, RepositoryException {
		return new LocalRepository(
				XMLTools.getTagContents(element, "alias", true),
				new File(XMLTools.getTagContents(element, "file", true)));
	}

	@Override
	public void delete() {
		RepositoryManager.getInstance(null).removeRepository(this);
	}

	@Override
	public boolean shouldSave() {
		return true;
	}
}

