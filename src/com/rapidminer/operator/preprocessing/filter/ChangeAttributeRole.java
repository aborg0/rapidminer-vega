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
package com.rapidminer.operator.preprocessing.filter;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * <p>
 * This operator can be used to change the attribute type of an attribute of the input example set.
 * If you want to change the attribute name you should use the {@link ChangeAttributeName} operator.
 * </p>
 *
 * <p>
 * The target type indicates if the attribute is a regular attribute (used by learning operators) or a
 * special attribute (e.g. a label or id attribute). The following target
 * attribute types are possible:
 * </p>
 * <ul>
 * <li>regular: only regular attributes are used as input variables for learning tasks</li>
 * <li>id: the id attribute for the example set</li>
 * <li>label: target attribute for learning</li>
 * <li>prediction: predicted attribute, i.e. the predictions of a learning scheme</li>
 * <li>cluster: indicates the membership to a cluster</li>
 * <li>weight: indicates the weight of the example</li>
 * <li>batch: indicates the membership to an example batch</li>
 * </ul>
 * <p>
 * Users can also define own attribute types by simply using the desired name.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class ChangeAttributeRole extends AbstractDataProcessing {

	/** The parameter name for &quot;The name of the attribute of which the type should be changed.&quot; */
	public static final String PARAMETER_NAME = "name";

	/** The parameter name for &quot;The target type of the attribute (only changed if parameter change_attribute_type is true).&quot; */
	public static final String PARAMETER_TARGET_ROLE = "target_role";

	private static final String REGULAR_NAME = "regular";

	private static final String[] TARGET_ROLES = new String[] { REGULAR_NAME, Attributes.ID_NAME, Attributes.LABEL_NAME, Attributes.PREDICTION_NAME, Attributes.CLUSTER_NAME, Attributes.WEIGHT_NAME, Attributes.BATCH_NAME };


	public ChangeAttributeRole(OperatorDescription description) {
		super(description);
		getExampleSetInputPort().addPrecondition(new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(this, PARAMETER_NAME)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		try {
			if (isParameterSet(PARAMETER_NAME)) {
				String name = getParameter(PARAMETER_NAME);
				AttributeMetaData amd = metaData.getAttributeByName(name);
				if (amd != null) {
					if (isParameterSet(PARAMETER_TARGET_ROLE)) {
						String role = getParameterAsString(PARAMETER_TARGET_ROLE);
						if (REGULAR_NAME.equals(role)) {
							amd.setRegular();
						} else {
							AttributeMetaData oldRole = metaData.getAttributeByRole(role);
							if (oldRole != null && oldRole != amd)
								metaData.removeAttribute(oldRole);
							amd.setRole(role);
						}
					}				
				}			
			}
		} catch (UndefinedParameterError e) {			
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {		 
		String name = getParameterAsString(PARAMETER_NAME);
		Attribute attribute = exampleSet.getAttributes().get(name);

		if (attribute == null) {
			throw new UserError(this, 111, name);
		}

		exampleSet.getAttributes().remove(attribute);
		String newType = getParameterAsString(PARAMETER_TARGET_ROLE);
		if ((newType == null) || (newType.trim().length() == 0))
			throw new UserError(this, 205, PARAMETER_TARGET_ROLE);
		if (newType.equals(REGULAR_NAME)) {
			exampleSet.getAttributes().addRegular(attribute);
		} else {
			exampleSet.getAttributes().setSpecialAttribute(attribute, newType);
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_NAME, "The name of the attribute whose role should be changed.", getExampleSetInputPort(), false, false));
		ParameterType type = new ParameterTypeStringCategory(PARAMETER_TARGET_ROLE, "The target role of the attribute (only changed if parameter change_attribute_type is true).", TARGET_ROLES, TARGET_ROLES[0]);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
