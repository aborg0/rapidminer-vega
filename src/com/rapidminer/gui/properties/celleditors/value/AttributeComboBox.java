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
package com.rapidminer.gui.properties.celleditors.value;

import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.tools.autocomplete.AutoCompleteComboBoxAddition;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.MetaDataChangeListener;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterTypeAttribute;

/**
 * Autocompletion combo box that observes an input port so it can update itself whenever the meta data changes.
 * 
 * @author Simon Fischer
 * 
 */
public class AttributeComboBox extends JComboBox {

	private static final long serialVersionUID = 1L;

	private static class AttributeComboBoxModel extends DefaultComboBoxModel implements MetaDataChangeListener {
		private static final long serialVersionUID = 1L;

		private ParameterTypeAttribute attributeType;
		private Vector<String> attributes = null;
		private MetaData lastMetaData = null;

		public AttributeComboBoxModel(ParameterTypeAttribute attributeType) {
			this.attributeType = attributeType;
		}

		@Override
		public int getSize() {
			if (attributeType.getInputPort() != null) {
				if (lastMetaData == null && lastMetaData != attributeType.getInputPort().getMetaData()) {
					attributes = attributeType.getAttributeNames();
					lastMetaData = attributeType.getInputPort().getMetaData();
					if (attributes != null) {
						fireContentsChanged(0, 0, attributes.size());
					}
				}
				if (attributes != null) {
					return attributes.size();
				}
			}
			return 0;
		}

		@Override
		public Object getElementAt(int index) {
			InputPort inputPort = attributeType.getInputPort();
			if (inputPort != null) {
				if (lastMetaData == null && lastMetaData != inputPort.getMetaData()) {
					attributes = attributeType.getAttributeNames();
					lastMetaData = inputPort.getMetaData();
					fireContentsChanged(0, 0, attributes.size());
				}
				return attributes.get(index);
			}
			return null;
		}

		/**
		 * This method will cause this model to register as a MetaDataChangeListener on the given input port. Attention!
		 * Make sure, it will be proper unregistered to avoid a memory leak!
		 */
		protected void registerListener() {
			InputPort inputPort = attributeType.getInputPort();
			if (inputPort != null)
				inputPort.registerMetaDataChangeListener(this);
		}

		/**
		 * This method will unregister this model from the InputPort.
		 */
		protected void unregisterListener() {
			InputPort inputPort = attributeType.getInputPort();
			if (inputPort != null)
				inputPort.removeMetaDataChangeListener(this);
		}

		@Override
		public void informMetaDataChanged(MetaData newMetadata) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					attributes = attributeType.getAttributeNames();
					fireContentsChanged(0, 0, attributes.size());
				}
			});
		}
	}

	private AttributeComboBoxModel model;

	public AttributeComboBox(ParameterTypeAttribute type) {
		super(new AttributeComboBoxModel(type));
		model = (AttributeComboBoxModel) getModel();
		AutoCompleteComboBoxAddition autoCompleteCBA = new AutoCompleteComboBoxAddition(this);
		autoCompleteCBA.setCaseSensitive(true);
	}

	@Override
	public void addNotify() {
		super.addNotify();
		model.registerListener();
	}

	@Override
	public void removeNotify() {
		super.removeNotify();
		model.unregisterListener();
	}
}
