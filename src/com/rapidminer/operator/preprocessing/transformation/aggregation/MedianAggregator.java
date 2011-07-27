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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is an {@link Aggregator} for the {@link MeanAggregationFunction}
 * 
 * @author Sebastian Land
 */
public class MedianAggregator extends NumericalAggregator {

    private static final int BUFFER_SIZE = 65536;

    private static class MedianListElement {
        private double[] elements = new double[BUFFER_SIZE];
    }

    private List<MedianListElement> elements = new ArrayList<MedianListElement>();
    private int currentIndex = 0;
    private int count = 0;
    private MedianListElement currentElement = null;

    public MedianAggregator(AggregationFunction function) {
        super(function);
    }

    @Override
    public void count(double value) {
        currentIndex = count % BUFFER_SIZE;
        if (currentIndex == 0) {
            currentElement = new MedianListElement();
            elements.add(currentElement);
        }

        currentElement.elements[currentIndex] = value;

        count++;
    }

    @Override
    public double getValue() {
        // first derive full copy of all values into one single array
        double[] allValues = new double[count];

        // therefore copy all full elements
        for (int i = 0; i < elements.size() - 1; i++) {
            System.arraycopy(elements.get(i).elements, 0, allValues, i * BUFFER_SIZE, BUFFER_SIZE);
        }

        // for the last only copy the filled values
        int numberOfValues = count % BUFFER_SIZE;
        System.arraycopy(elements.get(elements.size() - 1).elements, 0, allValues, (elements.size() - 1) * BUFFER_SIZE, numberOfValues);

        // now sort array
        Arrays.sort(allValues);

        // finally set value, either as center or as mean of centers
        double value = allValues[count / 2];
        if (count % 2 == 0)
            value = (value + allValues[count / 2 - 1]) / 2;

        return value;
    }
}
