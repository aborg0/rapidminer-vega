/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
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
package com.rapidminer.operator.learner.tree;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SortedExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.tree.criterions.Criterion;
import com.rapidminer.tools.Tools;

/**
 * Calculates the best split point for numerical attributes according to 
 * a given criterion.
 * 
 * @author Ingo Mierswa
 */
public class NumericalSplitter {
	
    private Criterion criterion;
    
    
    public NumericalSplitter(Criterion criterion) {
        this.criterion = criterion;
    }
    
    public double getBestSplit(ExampleSet inputSet, Attribute attribute) throws OperatorException {
        SortedExampleSet exampleSet = new SortedExampleSet((ExampleSet)inputSet.clone(), attribute, SortedExampleSet.INCREASING);
        Attribute labelAttribute = exampleSet.getAttributes().getLabel();
        double oldLabel = Double.NaN;
        double bestSplit = Double.NaN;
        double lastValue = Double.NaN;
        double bestSplitBenefit = Double.NEGATIVE_INFINITY;
      
        Example lastExample = null;
        if (this.criterion.supportsIncrementalCalculation()) {
        	this.criterion.startIncrementalCalculation(exampleSet);
        }
        
        for (Example e : exampleSet) {
        	double currentValue = e.getValue(attribute);
        	
        	double label = e.getValue(labelAttribute);   
    		if (this.criterion.supportsIncrementalCalculation()) {
    			if (lastExample != null) 
    				this.criterion.swapExample(lastExample);
    			lastExample = e;
    			if ((Double.isNaN(oldLabel)) || (oldLabel != label)) {
    				if (!Tools.isEqual(currentValue, lastValue)) {
    					double benefit = this.criterion.getIncrementalBenefit();

    					if (benefit > bestSplitBenefit) {
    						bestSplitBenefit = benefit;
    						bestSplit = (lastValue + currentValue) / 2.0d;
    					}
    					oldLabel = label;
    				}
        		}
        	} else if ((Double.isNaN(oldLabel)) || (oldLabel != label)) {
        		if (!Tools.isEqual(currentValue, lastValue)) {
        			double splitValue = (lastValue + currentValue) / 2.0d;
        			double benefit = this.criterion.getNumericalBenefit(exampleSet, attribute, splitValue);
        			if (benefit > bestSplitBenefit) {
        				bestSplitBenefit = benefit;
        				bestSplit = splitValue;
        			}
        			oldLabel = label;
        		}
        	}
    		
            lastValue = currentValue;
        }
        return bestSplit;
    }
}
