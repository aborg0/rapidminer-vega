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
import com.rapidminer.example.Example;
import com.rapidminer.example.table.DataRow;

/**
 * This is an implementation of a Aggregator for numerical attributes. It takes over
 * the handling of missing values.
 * @author Sebastian Land
 */
public abstract class NumericalAggregator implements Aggregator {

    private Attribute sourceAttribute;
    private boolean ignoreMissings;
    private boolean isMissing = false;

    public NumericalAggregator(AggregationFunction function) {
        this.sourceAttribute = function.getSourceAttribute();
        this.ignoreMissings = function.isIgnoringMissings();
    }

    @Override
    public final void count(Example example) {
        // check whether we have to count at all
        if (!isMissing || ignoreMissings) {
            double value = example.getValue(sourceAttribute);
            if (isMissing && !ignoreMissings || Double.isNaN(value)) {
                isMissing = true;
            } else {
                count(value);
            }
        }
    }

    /**
     * This method will count the given numerical value. This method will not be called in
     * cases, where the examples value for the given source Attribute is unknown.
     * Subclasses of this class will in this cases return either NaN if ignoreMissings is false,
     * or will return the value as if the examples with the missing aren't present at all.
     */
    protected abstract void count(double value);

    @Override
    public final void set(Attribute attribute, DataRow row) {
        if (isMissing && !ignoreMissings)
            row.set(attribute, Double.NaN);
        else
            row.set(attribute, getValue());
    }

    /**
     * This method has to return the numerical value of this aggregator.
     */
    protected abstract double getValue();

}
