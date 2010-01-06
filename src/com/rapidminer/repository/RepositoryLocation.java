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
package com.rapidminer.repository;

import java.util.Arrays;
import java.util.LinkedList;

/** A location in a repository. Format:
 * 
 *  //Repository/path/to/object
 *
 * All constructors throw IllegalArugmentExceptions if names are malformed, contain illegal 
 * characters etc.
 * 
 * @author Simon Fischer
 *
 */
public class RepositoryLocation {

	public static final char SEPARATOR = '/';
	public static final String REPOSITORY_PREFIX = "//";
	
	private String repositoryName;
	private String[] path;
	
	
	/** Constructs a RepositoryLocation from a string of the form
	 *  //Repository/path/to/object. */
	public RepositoryLocation(String absoluteLocation) {
		if (isAbsolute(absoluteLocation)) {
			initializeFromAbsoluteLocation(absoluteLocation);
		} else {
			repositoryName = null;
			initializeAbsolutePath(absoluteLocation);
		}
	}
	
	/** Creates a RepositoryLocation for a given repository and a set of path components
	 *  which will be concatenated by a /. */
	public RepositoryLocation(String repositoryName, String[] pathComponents) {
		this.repositoryName = repositoryName;
		this.path = pathComponents;
		String tmp;
		tmp  = "" + SEPARATOR + SEPARATOR + repositoryName;
		for (String component : pathComponents) {
			checkName(component);
			tmp += SEPARATOR + component ;						
		}
	}
	
	/** Appends a child entry to a given parent location. Child can be composed
	 *  of subcomponents separated by /. Dots ("..") will resolve to the parent 
	 *  folder. */
	public RepositoryLocation(RepositoryLocation parent, String childName) {		
		if (isAbsolute(childName)) {
			initializeFromAbsoluteLocation(childName);
		} else if (childName.startsWith(""+SEPARATOR)) {
			this.repositoryName = parent.repositoryName;
			initializeAbsolutePath(childName);
		} else {
			this.repositoryName = parent.repositoryName;
			String[] components = childName.split(""+SEPARATOR);
			LinkedList<String> newComponents = new LinkedList<String>();
			newComponents.addAll(Arrays.asList(parent.path));
			for (String component : components) {
				if (".".equals(component)) {
					// do nothing
				} else if ("..".equals(component)) {
					if (!newComponents.isEmpty()) {
						newComponents.removeLast();
					} else {
						throw new IllegalArgumentException("Cannot resolve relative location '"+childName+"' with respect to '"+parent+"': Too many '..'");
					}
				} else {
					newComponents.add(component);
				}
			}		
			String tmp = "";
			for (String component : newComponents) {
				tmp += SEPARATOR + component;
			}
			this.path = newComponents.toArray(new String[newComponents.size()]);
		}
	}

	private void initializeFromAbsoluteLocation(String absoluteLocation) {
		if (!isAbsolute(absoluteLocation)) {
			throw new IllegalArgumentException("Location is not absolute: '"+absoluteLocation+"'!");
		}
		
		String tmp = absoluteLocation.substring(2);
		int nextSlash = tmp.indexOf(RepositoryLocation.SEPARATOR);
		if (nextSlash != -1) {
			repositoryName = tmp.substring(0, nextSlash);
		} else {
			throw new IllegalArgumentException("Malformed repositoy location: "+absoluteLocation+": path component missing.");
		}
		initializeAbsolutePath(tmp.substring(nextSlash));		
	}
	
	private void initializeAbsolutePath(String path) {
		if (!path.startsWith(""+SEPARATOR)) {
			throw new IllegalArgumentException("No absolute path: "+path);
		}
		path = path.substring(1);
		this.path = path.split(""+SEPARATOR);
	}
	
	private static void checkName(String name) {
		if (name.contains(""+SEPARATOR)) {
			throw new IllegalArgumentException("Names must not contain '"+SEPARATOR+"'.");
		}
	}
	
	/** Returns the absolute location of this RepoositoryLocation. */
	public String getAbsoluteLocation() {
		StringBuilder b = new StringBuilder();
		b.append(REPOSITORY_PREFIX);
		b.append(repositoryName);
		for (String component : path) {
			b.append(SEPARATOR);
			b.append(component);
		}
		return b.toString();
	}
	
	/** Returns the repository associated with this location. */
	public Repository getRepository() {
		return RepositoryManager.getInstance().getRepository(repositoryName);		
	}

	/** Returns the name of the repository associated with this location. */
	public String getRepositoryName() {
		return repositoryName;		
	}
	
	/** Returns the path within the repository. */
	public String getPath() {
		StringBuilder b = new StringBuilder();
		for (String component : path) {
			b.append(SEPARATOR);
			b.append(component);
		}
		return b.toString();
	}
	
	/** Locates the corresponding entry in the repository. */
	public Entry locateEntry() throws RepositoryException {
		Repository repos = getRepository();		
		if (repos != null) {
			Entry entry = repos.locate(getPath());
			return entry;
		} else {			
			return null;
		}
	}

	/** Returns the last path component. */
	public String getName() {
		return path[path.length-1];
	}

	public RepositoryLocation parent() {
		if (path.length == 0) {
			// we are at a root
			return null; 
		} else {
			String[] pathCopy = new String[path.length-1];
			System.arraycopy(path, 0, pathCopy, 0, path.length-1);
			return new RepositoryLocation(this.repositoryName, pathCopy);
		}
		
	}
	
	@Override
	public String toString() {
		return getAbsoluteLocation();
	}
	
	/** Assume absoluteLocation == "//MyRepos/foo/bar/object" and 
	 *  relativeToFolder=//MyRepos/foo/baz/, then this method will return
	 *  ../bar/object. */
	public String makeRelative(RepositoryLocation relativeToFolder) {
		// can only do something if repositories match.
		if (!this.repositoryName.equals(relativeToFolder.repositoryName)) {
			return getAbsoluteLocation();
		}
		
		int min = Math.min(this.path.length, relativeToFolder.path.length);		
		// find common prefix
		int i = 0;
		while ((i < min) && (this.path[i].equals(relativeToFolder.path[i]))) {
			i++;
		}
		String result = "";
		// add one ../ for each excess component in relativeComponent which we have to leave
		for (int j = i; j < relativeToFolder.path.length; j++) {
			result += ".." + RepositoryLocation.SEPARATOR;
		}
		// add components from each excess absoluteComponent
		for (int j = i; j < this.path.length; j++) {
			result += this.path[j];
			if (j < this.path.length-1) {
				 result += RepositoryLocation.SEPARATOR;
			}
		}		
		return result;
	}

	public static boolean isAbsolute(String loc) {
		return loc.startsWith(RepositoryLocation.REPOSITORY_PREFIX);
	}

	/** Creates this folder and its parents. 
	 * @throws RepositoryException */
	public Folder createFoldersRecursively() throws RepositoryException {
		Entry entry = locateEntry();
		if (entry == null) {
			Folder folder = parent().createFoldersRecursively();
			Folder child = folder.createFolder(getName());
			return child;
		} else {
			if (entry instanceof Folder) {
				return (Folder) entry;
			} else {
				throw new RepositoryException(toString() + " is not a folder.");
			}
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj != null) && this.toString().equals(obj.toString());
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
}
