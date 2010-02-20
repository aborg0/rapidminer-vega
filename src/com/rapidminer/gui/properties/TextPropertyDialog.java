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
package com.rapidminer.gui.properties;

import com.rapidminer.gui.properties.celleditors.value.TextValueCellEditor;
import com.rapidminer.gui.tools.HTMLEditor;
import com.rapidminer.gui.tools.JavaEditor;
import com.rapidminer.gui.tools.PlainTextEditor;
import com.rapidminer.gui.tools.SQLEditor;
import com.rapidminer.gui.tools.XMLEditor;
import com.rapidminer.gui.tools.syntax.JEditTextArea;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;


/**
 * A Dialog displaying a {@link JEditTextArea}. This can be used to type some lengthy
 * text instead of the short text fields usually used for ParameterTypeStrings. This
 * dialog is used by the {@link TextValueCellEditor}.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class TextPropertyDialog extends PropertyDialog {

    private static final long serialVersionUID = 8574310060170861505L;

    private String text = null;
    
    private boolean ok = false;

    private JEditTextArea textArea = null;
    
    
    public TextPropertyDialog(final ParameterTypeText type, String text, Operator operator) {
        super(type, "text");
        this.text = text;

        // text area
        textArea = createTextArea(type.getTextType());
        textArea.setText(this.text);
        textArea.setBorder(createBorder());
        layoutDefault(textArea, NORMAL, makeOkButton(), makeCancelButton());
    }

    @Override
    protected void ok() {
    	this.ok = true;
        this.text = this.textArea.getText();
        dispose();
    }
    
    @Override
    protected void cancel() {
    	this.ok = false;
    	dispose();
    }
    
    @Override
	public boolean isOk() {
    	return this.ok;
    }
    
    public String getText() {
        return this.text;
    }
    
    private  JEditTextArea createTextArea(TextType type) {
        switch (type) {
        	case PLAIN: return new PlainTextEditor();
            case XML: return new XMLEditor();
            case HTML: return new HTMLEditor();
            case SQL: return new SQLEditor();
            case JAVA: return new JavaEditor();
        }
        return new PlainTextEditor();
    }
}
