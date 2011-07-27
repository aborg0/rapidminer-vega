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
import com.rapidminer.example.table.DoubleArrayDataRow;

/**
 * This class implements the Count Aggregation function. This will calculate the
 * number of examples within a group. Examples with a missing value in the attribute won't be counted. To count all
 * of them use the CountMAggreagationFunction.
 * TODO: Needs to be checked whether this is same as in old operator.
 * @author Sebastian Land
 */
public class CountMAggregationFunction extends NumericalAggregationFunction {

    public static final String FUNCTION_COUNT_M = "countM";

    public CountMAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings) {
        super(sourceAttribute, ignoreMissings, FUNCTION_COUNT_M, FUNCTION_SEPARATOR_OPEN, FUNCTION_SEPARATOR_CLOSE);
    }

    public CountMAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, String functionName, String separatorOpen, String separatorClose) {
        super(sourceAttribute, ignoreMissings, functionName, separatorOpen, separatorClose);
    }

    @Override
    public Aggregator createAggregator() {
        return new CountMAggregator(this);
    }

    @Override
    public boolean isCompatible() {
        return true;
    }

    @Override
    public void setDefault(Attribute attribute, DoubleArrayDataRow row) {
        row.set(attribute, 0);
    }
}
