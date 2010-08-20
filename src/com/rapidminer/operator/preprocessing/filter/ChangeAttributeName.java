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
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.MetaDataUnderspecifiedError;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * <p>
 * This operator can be used to rename an attribute of the input example set.
 * If you want to change the attribute type (e.g. from regular to id attribute or from label to regular etc.),
 * you should use the {@link ChangeAttributeType} operator.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class ChangeAttributeName extends AbstractDataProcessing {

	/** The parameter name for &quot;The old name of the attribute.&quot; */
	public static final String PARAMETER_OLD_NAME = "old_name";

	/** The parameter name for &quot;The new name of the attribute.&quot; */
	public static final String PARAMETER_NEW_NAME = "new_name";

	public ChangeAttributeName(OperatorDescription description) {
		super(description);
		getExampleSetInputPort().addPrecondition(new AttributeSetPrecondition(getExampleSetInputPort(),AttributeSetPrecondition.getAttributesByParameter(this, PARAMETER_OLD_NAME)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		try {
			if (isParameterSet(PARAMETER_OLD_NAME)) {
				String name = getParameter(PARAMETER_OLD_NAME);
				AttributeMetaData amd = metaData.getAttributeByName(name);
				if (amd == null) {
					switch (metaData.getAttributeSetRelation()) {				
					case SUPERSET:
					case EQUAL:
						getInputPort().addError(new SimpleMetaDataError(Severity.ERROR, getInputPort(), "missing_attribute", name));
						break;
					case SUBSET:
					case UNKNOWN:
					default:
						getInputPort().addError(new MetaDataUnderspecifiedError(getInputPort()));
						break;
					}
				} else {
					if (isParameterSet(PARAMETER_NEW_NAME)) {
						String newName = getParameterAsString(PARAMETER_NEW_NAME);
						if (metaData.containsAttributeName(newName) == MetaDataInfo.YES) {
							getInputPort().addError(new SimpleMetaDataError(Severity.ERROR, getInputPort(), "already_contains_attribute", newName));
						} else {
							amd.setName(getParameterAsString(PARAMETER_NEW_NAME));
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
		String oldName = getParameterAsString(PARAMETER_OLD_NAME);
		Attribute attribute = exampleSet.getAttributes().get(oldName);
		if (attribute == null) {
			throw new UserError(this, 111, oldName);
		}
		String newName = getParameterAsString(PARAMETER_NEW_NAME);
		attribute.setName(newName);
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_OLD_NAME, "The old name of the attribute.", getExampleSetInputPort(), false));
		types.add(new ParameterTypeString(PARAMETER_NEW_NAME, "The new name of the attribute.", false));
		return types;
	}
	
	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), ChangeAttributeName.class, null);
	}
}
