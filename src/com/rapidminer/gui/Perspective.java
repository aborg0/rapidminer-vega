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
package com.rapidminer.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.vlsolutions.swing.docking.DockingContext;
import com.vlsolutions.swing.docking.ws.Workspace;
import com.vlsolutions.swing.docking.ws.WorkspaceException;

/**
 * 
 * @author Simon Fischer
 *
 */
public class Perspective {

	private final String name;
	private final Workspace workspace = new Workspace();
	private boolean userDefined = false;;
	private final ApplicationPerspectives owner;
	
	public Perspective(ApplicationPerspectives owner, String name) {
		this.name = name;
		this.owner = owner;
	}

	public String getName() {
		return name;		
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public void store(DockingContext dockingContext) {
		try {
			workspace.loadFrom(dockingContext);
		} catch (WorkspaceException e) {
			LogService.getGlobal().logError("Cannot save workspace: "+e.toString());
			e.printStackTrace();
		}

	}
	protected void apply(DockingContext dockingContext) {
		try {
			workspace.apply(dockingContext);
		} catch (WorkspaceException e) {
			LogService.getGlobal().logError("Cannot apply workspace: "+e.toString());
			e.printStackTrace();			
		}
	}

	File getFile() {
		return ParameterService.getUserConfigFile("vlperspective-"+(isUserDefined()?"user-":"predefined-")+name+".xml");
	}
	
	public void save() {
		File file = getFile();
		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			workspace.writeXML(out);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Cannot save perspective to "+file+": "+e, e);
		} finally {	
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) { }
		}
	}
	
	public void load() {
		LogService.getRoot().fine("Loading perspective: "+getName());
		File file = getFile();
		if (!file.exists()) {
			return;
		}
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			workspace.readXML(in);
		} catch (Exception e) {
			
			if (!userDefined) {
				LogService.getRoot().log(Level.WARNING, "Cannot read perspective from "+file+": "+e+". Restoring default.", e);
				owner.restoreDefault(getName());
			} else {
				LogService.getRoot().log(Level.WARNING, "Cannot read perspective from "+file+": "+e+". Clearing perspective.", e);
				workspace.clear();
			}
		} finally {	
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) { }
		}
	}

	public void setUserDefined(boolean b) {
		this.userDefined  = b;		
	}

	public boolean isUserDefined() {
		return this.userDefined;		
	}

	public void delete() {
		File file = getFile();
		if (file.exists()) {
			file.delete();
		}
	}

}
