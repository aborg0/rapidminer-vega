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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel;

import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.io.ExcelExampleSource;

/**
 * 
 * @author Simon Fischer
 *
 */
public class AnnotationCellEditor extends DefaultCellEditor {

	public static final String NONE = "-";
	public static final String NAME = ExcelExampleSource.ANNOTATION_NAME;
	
	private static final long serialVersionUID = 1L;

	private static JComboBox makeComboBox() {
		Vector<String> values = new Vector<String>();
		values.add(NONE);
		values.add(NAME);
		for (String a : Annotations.ALL_KEYS_ATTRIBUTE) {
			values.add(a);
		}		
		return new JComboBox(values);
	}

	public AnnotationCellEditor() {
		super(makeComboBox());
	}



}
