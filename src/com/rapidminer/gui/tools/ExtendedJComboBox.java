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
package com.rapidminer.gui.tools;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * A combo box which can use a predefined preferred size. Can also show the full value as
 * tool tip in cases where the strings were too short.
 * 
 * @author Ingo Mierswa
 */
public class ExtendedJComboBox extends JComboBox {

	private static final long serialVersionUID = 8320969518243948543L;

	private static class ExtendedComboBoxRenderer extends BasicComboBoxRenderer {

		private static final long serialVersionUID = -6192190927539294311L;

		@Override
		public Component getListCellRendererComponent(JList list, Object value,	int index, boolean isSelected, boolean cellHasFocus) {
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
				if (index >= 0) {
					list.setToolTipText((value == null) ? null : value.toString());
				}
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setFont(list.getFont());
			setText((value == null) ? "" : value.toString());
			return this;
		}
	}


	private int preferredWidth = -1;

	private int minimumWidth = -1;

	private boolean layingOut = false; 

	private boolean wide = true; 

	public ExtendedJComboBox(String[] values) {
		this(-1, -1, true, values);
	}

	public ExtendedJComboBox() {
		this(-1, -1);
	}

	public ExtendedJComboBox(int preferredWidth) {
		this(preferredWidth, -1);
	}

	public ExtendedJComboBox(int preferredWidth, int minimumWidth) {
		this(preferredWidth, minimumWidth, true);
	}

	public ExtendedJComboBox(int preferredWidth, int minimumWidth, boolean wide) {
		this(preferredWidth, minimumWidth, wide, new String[0]);
	}

	public ExtendedJComboBox(ComboBoxModel model) {
		this(-1, -1, true, model);
	}
	
	public ExtendedJComboBox(int preferredWidth, int minimumWidth, boolean wide, ComboBoxModel model) {
		super(model);
		this.preferredWidth = preferredWidth;
		this.minimumWidth = minimumWidth;
		this.wide = wide;
	}	
	
	public ExtendedJComboBox(int preferredWidth, int minimumWidth, boolean wide, String[] values) {
		super(values);
		this.preferredWidth = preferredWidth;
		this.minimumWidth = minimumWidth;
		this.wide = wide;

		setRenderer(new ExtendedComboBoxRenderer());
	}

	public boolean isWide() { 
		return wide; 
	}

	public void setWide(boolean wide) { 
		this.wide = wide; 
	} 

	@Override
	public Dimension getPreferredSize() {
		Dimension dim = super.getPreferredSize();
		if (this.preferredWidth != -1) {
			if (preferredWidth < dim.getWidth()) {
				return new Dimension(preferredWidth, (int)dim.getHeight());
			} else {
				return dim;
			}
		} else {
			return dim;
		}
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension dim = super.getMinimumSize();
		if (this.minimumWidth != -1) {
			if (minimumWidth < dim.getWidth()) {
				return new Dimension(minimumWidth, (int)dim.getHeight());
			} else {
				return dim;
			}
		} else {
			return dim;
		}
	}

	@Override
	public void doLayout(){ 
		try { 
			layingOut = true; 
			super.doLayout(); 
		} finally { 
			layingOut = false; 
		} 
	} 

	@Override
	public Dimension getSize(){ 
		Dimension dim = super.getSize(); 
		if (!layingOut && isWide()) 
			dim.width = Math.max(dim.width, super.getPreferredSize().width); 
		dim.width = Math.min(dim.width, Toolkit.getDefaultToolkit().getScreenSize().width - 40); 
		return dim; 
	} 
}
