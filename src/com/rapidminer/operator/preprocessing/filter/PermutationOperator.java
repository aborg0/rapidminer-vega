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

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowReader;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.RandomGenerator;


/** This operator creates a new, shuffled ExampleSet by making <em>a new copy</em>
 *  of the exampletable in main memory!
 *  Caution! System may run out of memory, if the example table is too large.
 *
 *  @author Sebastian Land, Ingo Mierswa
 */
public class PermutationOperator extends AbstractDataProcessing {

	public PermutationOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {         
		ExampleTable table = exampleSet.getExampleTable();

		// generate attribute list (clones)
		List<Attribute> attributeList = new ArrayList<Attribute>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			attributeList.add((Attribute)attribute.clone());
		}
		int toCopy = table.size();

		// generate new ExampleTable of size of old table
		MemoryExampleTable shuffledTable = new MemoryExampleTable(attributeList);

		// copy all dataRows
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		DataRow[] isCopied = new DataRow[toCopy];
		int areCopied = 0;
		DataRowReader reader = table.getDataRowReader();
		while (areCopied < toCopy) {
			int currentRow = (int)Math.round((random.nextDouble()*(toCopy-1)));
			if (isCopied[currentRow] == null){
				isCopied[currentRow] = reader.next();
				// increase counter of copied rows
				areCopied++;
			}
			checkForStop();
		}
		for (int i=0; i < toCopy; i++) {
			shuffledTable.addDataRow(isCopied[i]);
		}

		return shuffledTable.createExampleSet(exampleSet.getAttributes().specialAttributes());
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
	
	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), PermutationOperator.class, null);
	}
}
