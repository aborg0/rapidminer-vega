/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
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
package com.rapidminer.operator.preprocessing.transformation.aggregation;

import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.tools.Ontology;

/**
 * This class offers an implementation of {@link AggregationFunctionMetaDataProvider} interface and can be
 * parameterized to work for several {@link AggregationFunction}s on instanciation.
 * 
 * @author Sebastian Land
 */
public class DefaultAggregationFunctionMetaDataProvider implements AggregationFunctionMetaDataProvider {

    private boolean copyValueType = false;
    private int matchingValueType;
    private int resultValueType;
    private String aggregationFunctionName;
    private String functionName;
    private String separatorOpen;
    private String separatorClose;

    public DefaultAggregationFunctionMetaDataProvider(String aggregationFunctionName, String functionName, String separatorOpen, String separatorClose, int matchingValueType) {
        this(aggregationFunctionName, functionName, separatorOpen, separatorClose, matchingValueType, 0);
        this.copyValueType = true;
    }

    public DefaultAggregationFunctionMetaDataProvider(String aggregationFunctionName, String functionName, String separatorOpen, String separatorClose, int matchingValueType, int resultValueType) {
        this.aggregationFunctionName = aggregationFunctionName;
        this.functionName = functionName;
        this.separatorClose = separatorClose;
        this.separatorOpen = separatorOpen;
        this.matchingValueType = matchingValueType;
        this.resultValueType = resultValueType;
    }

    @Override
    public AttributeMetaData getTargetAttributeMetaData(AttributeMetaData sourceAttribute, InputPort port) {
        if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(sourceAttribute.getValueType(), matchingValueType) || sourceAttribute.getValueType() == Ontology.ATTRIBUTE_VALUE) {
            if (copyValueType)
                return new AttributeMetaData(functionName + separatorOpen + sourceAttribute.getName() + separatorClose, sourceAttribute.getValueType());
            else
                return new AttributeMetaData(functionName + separatorOpen + sourceAttribute.getName() + separatorClose, resultValueType);
        } else {
            // not matching type: Return null and register error
            port.addError(new SimpleMetaDataError(Severity.ERROR, port, "aggregation.incompatible_value_type", sourceAttribute.getName(), aggregationFunctionName, Ontology.VALUE_TYPE_NAMES[sourceAttribute.getValueType()], Ontology.VALUE_TYPE_NAMES[matchingValueType]));
            return null;
        }
    }

}
