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

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.TreeMap;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;

/**
 * This is an abstract class for all {@link AggregationFunction}s, that can be selected to
 * aggregate values of a certain group.
 * Each {@link AggregationFunction} must be able to provide a certain {@link Aggregator}, that
 * will count the examples of one single group and compute the aggregated value. So for example the {@link MeanAggregationFunction}
 * provides an {@link MeanAggregator}, that will calculate the mean on all examples delivered to him.
 * 
 * The list of the names of all available functions can be queried from the static method {@link #getAvailableAggregationFunctionNames()}.
 * With a name one can call the static method {@link #createAggregationFunction(String, Attribute)} to
 * create a certain aggregator for the actual counting.
 * 
 * Additional functions can be registered by calling {@link #registerNewAggregationFunction(String, Class)} from
 * extensions, preferable during their initialization. Please notice that there will be no warning prior process execution
 * if the extension is missing but the usage of it's function is still configured.
 *
 * @author Sebastian Land
 */
public abstract class AggregationFunction {

    public static final String FUNCTION_SEPARATOR_OPEN = "(";
    public static final String FUNCTION_SEPARATOR_CLOSE = ")";

    public static final Map<String, Class<? extends AggregationFunction>> AGGREATION_FUNCTIONS = new TreeMap<String, Class<? extends AggregationFunction>>();
    static {
        AGGREATION_FUNCTIONS.put("sum", SumAggregationFunction.class);
        AGGREATION_FUNCTIONS.put("median", MedianAggregationFunction.class);
        AGGREATION_FUNCTIONS.put("average", MeanAggregationFunction.class);
        AGGREATION_FUNCTIONS.put("variance", VarianceAggregationFunction.class);
        AGGREATION_FUNCTIONS.put("standard_deviation", StandardDeviationAggregationFunction.class);
        AGGREATION_FUNCTIONS.put("count (ignoring missings)", CountIgnoringMissingsAggregationFunction.class);
        AGGREATION_FUNCTIONS.put("count (including missings)", CountIncludingMissingsAggregationFunction.class);
        AGGREATION_FUNCTIONS.put("count", CountAggregationFunction.class);
        AGGREATION_FUNCTIONS.put("minimum", MinAggregationFunction.class);
        AGGREATION_FUNCTIONS.put("maximum", MaxAggregationFunction.class);

        AGGREATION_FUNCTIONS.put("log product", LogProductAggregationFunction.class);
        AGGREATION_FUNCTIONS.put("product", ProductAggregationFunction.class);

        // Nominal Aggregations
        AGGREATION_FUNCTIONS.put("mode", ModeAggregationFunction.class);
        AGGREATION_FUNCTIONS.put("least", LeastAggregationFunction.class);
        AGGREATION_FUNCTIONS.put("least (only occurring)", LeastOccurringAggregationFunction.class);
    }

    private Attribute sourceAttribute;
    private boolean isIgnoringMissings;
    private boolean isCountingOnlyDistinct;

    public AggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDistinct) {
        this.sourceAttribute = sourceAttribute;
        this.isIgnoringMissings = ignoreMissings;
        this.isCountingOnlyDistinct = countOnlyDistinct;
    }

    /**
     * This returns the attribute this aggregation function will derive the data from.
     */
    public Attribute getSourceAttribute() {
        return sourceAttribute;
    }

    /**
     * This returns the attribute that will be created in the resulting {@link ExampleSet} to
     * get the aggregated values for each group.
     */
    public abstract Attribute getTargetAttribute();

    /**
     * This will return the {@link Aggregator} object that computes the value of this
     * particular {@link AggregationFunction} for a specific group.
     */
    public abstract Aggregator createAggregator();

    /**
     * This determines, if any missing values will be just ignored or counted with the
     * respective aggregation function. Some functions might cope with that, others will
     * just turn to be NaN.
     */
    public boolean isIgnoringMissings() {
        return isIgnoringMissings;
    }


    /**
     * This determines, if values are counted only once, if occurring more than once. Please note
     * that will increase the memory load drastically on numerical attributes.
     */
    public boolean isCountingOnlyDistinct() {
        return isCountingOnlyDistinct;
    }

    /**
     * This will return whether this {@link AggregationFunction} is compatible with the given
     * sourceAttribute.
     */
    public abstract boolean isCompatible();

    /**
     * This will create the {@link AggregationFunction} with the given name for the given
     * source Attribute. This method might return
     */
    public static final AggregationFunction createAggregationFunction(String name, Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDistinct) throws OperatorException {
        Class<? extends AggregationFunction> aggregationFunctionClass = AGGREATION_FUNCTIONS.get(name);
        if (aggregationFunctionClass == null)
            throw new UserError(null, "aggregation.illegal_function_name", name);
        try {
            Constructor<? extends AggregationFunction> constructor = aggregationFunctionClass.getConstructor(Attribute.class, boolean.class, boolean.class);
            return constructor.newInstance(sourceAttribute, ignoreMissings, countOnlyDistinct);
        } catch (Exception e) {
            throw new RuntimeException("All implementations of AggregationFunction need to have a constructor accepting an Attribute and boolean. Other reasons for this error may be class loader problems.", e);
        }
    }

    /**
     * This method will fill in the default value of this aggregation function. It has to
     * maintain the mapping, if the function is nominal.
     * The default value will be a NaN. Every subclass that wants to change this, has to override
     * this method.
     */
    public void setDefault(Attribute attribute, DoubleArrayDataRow row) {
        row.set(attribute, Double.NaN);
    }

    /**
     * This method will return the array containing the names of all available
     * aggregation functions. The names are sorted according to natural ordering.
     */
    public static String[] getAvailableAggregationFunctionNames() {
        String[] names = new String[AGGREATION_FUNCTIONS.size()];
        int i = 0;
        for (String name: AGGREATION_FUNCTIONS.keySet()) {
            names[i] = name;
            i++;
        }

        return names;
    }

    /**
     * With this method extensions might register additional aggregation functions if needed.
     */
    public static void registerNewAggregationFunction(String name, Class<? extends AggregationFunction> clazz) {
        AGGREATION_FUNCTIONS.put(name, clazz);
    }

}