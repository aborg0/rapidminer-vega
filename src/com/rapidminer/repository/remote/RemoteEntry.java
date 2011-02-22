/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Action;

import com.rapid_i.repository.wsimport.AccessRights;
import com.rapid_i.repository.wsimport.EntryResponse;
import com.rapid_i.repository.wsimport.Response;
import com.rapidminer.gui.actions.BrowseAction;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
/**
 * @author Simon Fischer
 */
public abstract class RemoteEntry implements Entry {

	private static final Logger LOGGER = Logger.getLogger(RemoteEntry.class.getName());
	
	private RemoteRepository repository;	
	private RemoteFolder containingFolder;	
	private final String owner;	
	private String location;
	private String name;
	
	RemoteEntry(String location) {
		if (location == null) {
			throw new NullPointerException("Location cannot be null");
		}
		this.location = location;
		int lastSlash = location.lastIndexOf('/');
		if (lastSlash == -1) {
			name = location;
		} else {
			name = location.substring(lastSlash+1);
		}
		this.owner ="none";
	}
	
	RemoteEntry(EntryResponse response, RemoteFolder container, RemoteRepository repository) {
		this.location = response.getLocation();
		this.owner = response.getUser();
		this.containingFolder = container;
		this.repository = repository;
		int lastSlash = location.lastIndexOf('/');
		if (lastSlash == -1) {
			name = location;
		} else {
			name = location.substring(lastSlash+1);
		}
		if (location == null) {
			throw new NullPointerException("Location cannot be null");
		}
		if (repository != null) {
			repository.register(this);
		}
	}
	
	void setRepository(RemoteRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public Folder getContainingFolder() {
		return containingFolder;
	}

	@Override
	public String getDescription() {
		return "Remote entry at "+location;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOwner() {
		return owner;
	}


	@Override
	public boolean isReadOnly() {		
		return false;
	}

	@Override
	public boolean rename(String newName) throws RepositoryException {
		EntryResponse response = getRepository().getRepositoryService().rename(getPath(), newName);
		if (response.getStatus() == 0) {
			this.location = response.getLocation();
			int lastSlash = this.location.lastIndexOf('/');
			if (lastSlash == -1) {
				this.name = this.location;
			} else {
				this.name = this.location.substring(lastSlash+1);
			}
			getRepository().fireEntryRenamed(this);
			return true;
		} else {
			throw new RepositoryException(response.getErrorMessage());
		}
	}

	public final RemoteRepository getRepository() {
		return repository;
	}

	final String getPath() {
		return location;
	}
	
	protected static Logger getLogger() {
		return LOGGER;
	}
	
	@Override
	public RepositoryLocation getLocation() {
		try {
			return new RepositoryLocation(""+RepositoryLocation.REPOSITORY_PREFIX + getRepository().getName() + location);
		} catch (MalformedRepositoryLocationException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void delete() throws RepositoryException {				
		Response response = getRepository().getRepositoryService().deleteEntry(getLocation().getPath());
		if (response.getStatus() != 0) {
			throw new RepositoryException(response.getErrorMessage());
		}
		((RemoteFolder)getContainingFolder()).removeChild(this);
	}
	
	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		Response response = getRepository().getRepositoryService().move(getPath(), ((RemoteFolder) newParent).getPath());
		if (response.getStatus() != 0) {
			throw new RepositoryException(response.getErrorMessage());
		}
		containingFolder.removeChild(this);
		if (this instanceof Folder) {
			((RemoteFolder)newParent).getSubfolders().add((Folder) this);
		} else {
			((RemoteFolder)newParent).getDataEntries().add((DataEntry) this);
		}		
		this.containingFolder = (RemoteFolder) newParent;
		getRepository().fireEntryAdded(this, newParent);
		return true;
	}
	
	@Override
	public Collection<Action> getCustomActions() {
		List<Action> actions= new LinkedList<Action>();
		actions.add(new AccessRightsAction(this));
		actions.add(new BrowseAction("remoteprocessviewer.browse", getRepository().getURIForResource(location)));
		return actions;
	}
	
	/** Note: This method contacts the server and may be slow. Invoke in background. */
	public List<AccessRights> getAccessRights() throws RepositoryException {
		return getRepository().getRepositoryService().getAccessRights(location);
	}

	public void setAccessRights(List<AccessRights> accessRights) throws RepositoryException {
		getRepository().getRepositoryService().setAccessRights(getPath(), accessRights);		
	}
}
