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
package com.rapidminer.operator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.Tools;


/**
 * An adapter class for the interface {@link ResultObject}. Implements most
 * methods and can be used if the subclass does not need to extend other
 * classes. The method {@link #toResultString()} delivers the return value of
 * {@link #toString()}. The visualization components for the graphical user
 * interface is simply the HTML representation of the result string. If a
 * subclass also implements {@link Saveable} an action for Saving will
 * automatically be added to the actions list.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public abstract class ResultObjectAdapter extends AbstractIOObject implements ResultObject, LoggingHandler, Saveable {

	private static final long serialVersionUID = -8621885253590411373L;

	//private transient List<Action> actions;

	public ResultObjectAdapter() {
		//initActions();
	}

	/** The default implementation returns the classname without package. */
	public String getName() {
		return this.getClass().getSimpleName();
	}

	/** Returns true. 
	 * 
	 * @deprecated All objects can now always be saved, action concept for objects removed.
	 */
	@Deprecated
	public boolean isSavable() {
		return false;
	}

	/** Saves the object into the given file by using the {@link #write(OutputStream)} 
	 *  method of {@link IOObject} (XML format). */
	public void save(final File file) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			write(fos);
		} finally {
			if (fos != null)
				fos.close();
		}
	}

	/**
	 * Adds an action to the list of Java Swing Actions which will shown in the
	 * visualization component. If the class implements Saveable an action for
	 * saving is already added.
	 * 
	 * @deprecated Action concept for GUI components removed from result objects
	 */
	@Deprecated
	protected void addAction(Action a) {}

	/**
	 * Returns a list of all actions which can be performed for this result
	 * object.
	 * 
	 * @deprecated Action concept for GUI components removed from result objects
	 */
	@Deprecated
	public List<Action> getActions() {
		return new LinkedList<Action>();
	}

	/**
	 * The default implementation simply returns the result of the method
	 * {@link #toString()}.
	 */
	public String toResultString() {
		return toString();
	}

	/** Returns null. Subclasses might want to override this method and returns an appropriate
	 *  icon. */
	public Icon getResultIcon() {
		return null;
	}

	/**
	 * Encodes the given String as HTML. Only linebreaks and less then and
	 * greater than will be encoded.
	 */
	public static String toHTML(String string) {
		String str = string;
		str = str.replaceAll(">", "&gt;");
		str = str.replaceAll("<", "&lt;");
		str = str.replaceAll(Tools.getLineSeparator(), "<br>");
		return str;
	}


	@Override
	public void log(String message, int level) {
		getLog().log(message, level);
	}
	@Override
	public void log(String message) {
		getLog().log(getName() + ": " + message);
	}
	@Override
	public void logNote(String message) {
		getLog().logNote(getName() + ": " + message);
	}
	@Override
	public void logWarning(String message) {
		getLog().logWarning(getName() + ": " + message);
	}
	@Override
	public void logError(String message) {
		getLog().logError(getName() + ": " + message);
	}
}
