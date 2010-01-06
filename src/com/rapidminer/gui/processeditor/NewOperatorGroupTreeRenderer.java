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
package com.rapidminer.gui.processeditor;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.GroupTree;


/**
 * The renderer for the group tree (displays a small group icon).
 * 
 * @author Ingo Mierswa
 */
public class NewOperatorGroupTreeRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = -6092290820461444236L;

    public NewOperatorGroupTreeRenderer() {
		setLeafIcon(getDefaultClosedIcon());
    }
    
    @Override
	public Component getTreeCellRendererComponent(JTree tree, 
                                                  Object value, 
                                                  boolean isSelected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {

        
        if (value instanceof GroupTree) {
        	GroupTree groupTree = (GroupTree)value;
        	setToolTipText("This group contains all operators of the group '" + groupTree.getName() + "'.");
        	return super.getTreeCellRendererComponent(tree, groupTree.toString(), isSelected, expanded, leaf, row, hasFocus);
        } else {
        	OperatorDescription op = (OperatorDescription)value;
        	setToolTipText(null);
        	JLabel label = (JLabel)super.getTreeCellRendererComponent(tree, op.getName(), isSelected, expanded, leaf, row, hasFocus);
        	label.setIcon(op.getSmallIcon());
        	if (op.getDeprecationInfo() != null) {
        		label.setEnabled(false);
        	}
        	return label;
        }        
    }
}
