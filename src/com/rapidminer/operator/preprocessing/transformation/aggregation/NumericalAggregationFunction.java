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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.AttributeFactory;

/**
 * This class implements the Sum Aggregation function. This will calculate the
 * sum of a source attribute for each group.
 * 
 * @author Sebastian Land
 */
public abstract class NumericalAggregationFunction extends AggregationFunction {

    private Attribute targetAttribute;

    public NumericalAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, String functionName, String separatorOpen, String separatorClose) {
        super(sourceAttribute, ignoreMissings);
        this.targetAttribute = AttributeFactory.createAttribute(functionName + separatorOpen + getSourceAttribute().getName() + separatorClose, getSourceAttribute().getValueType());
    }

    @Override
    public Attribute getTargetAttribute() {
        return targetAttribute;
    }

    @Override
    public boolean isCompatible() {
        return getSourceAttribute().isNumerical();
    }

}
