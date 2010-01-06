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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
/**
 * @author Simon Fischer
 */
public interface Repository extends Folder {
	
	public void addRepositoryListener(RepositoryListener l);
	
	public void removeRepositoryListener(RepositoryListener l);

	public Entry locate(String string) throws RepositoryException;

	/** Returns some user readable information about the state of this repository. */
	public String getState();
	
	/** Returns a piece of XML to store the repository in a configuration file. */
	public Element createXML(Document doc);

}
