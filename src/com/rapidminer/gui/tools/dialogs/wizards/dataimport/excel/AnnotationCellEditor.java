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
