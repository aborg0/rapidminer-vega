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
package com.rapidminer.operator.performance.cost;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.MeasuredPerformance;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeMatrix;
import com.rapidminer.tools.Ontology;

/**
 * This operator provides the ability to evaluate classification costs. Therefore a cost matrix might be specified,
 * denoting the costs for every possible classification outcome: predicted label x real label. Costs will be minimized
 * during optimization.
 * 
 * @author Sebastian Land
 */
public class CostEvaluator extends Operator {

	private static final String PARAMETER_COST_MATRIX = "cost_matrix";
	private static final String PARAMETER_KEEP_EXAMPLE_SET = "keep_exampleSet";

	private InputPort exampleSetInput = getInputPorts().createPort("example set");

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort performanceOutput = getOutputPorts().createPort("performance");

	private double lastCosts = Double.NaN;

	public CostEvaluator(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.ATTRIBUTE_VALUE, Attributes.LABEL_NAME));
		getTransformer().addGenerationRule(performanceOutput, PerformanceVector.class);
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);

		addValue(new ValueDouble("costs", "The last costs.") {
			@Override
			public double getDoubleValue() {
				return lastCosts;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData();
		Attribute label = exampleSet.getAttributes().getLabel();
		if (label != null) {
			if (label.isNominal()) {
				double[][] costMatrix = getParameterAsMatrix(PARAMETER_COST_MATRIX);
				MeasuredPerformance criterion = new ClassificationCostCriterion(costMatrix, label, exampleSet.getAttributes().getPredictedLabel());
				PerformanceVector performance = new PerformanceVector();
				performance.addCriterion(criterion);
				// now measuring costs
				criterion.startCounting(exampleSet, false);
				for (Example example : exampleSet) {
					criterion.countExample(example);
				}

				// setting logging value
				lastCosts = criterion.getAverage();
				
				exampleSetOutput.deliver(exampleSet);
				performanceOutput.deliver(performance);
			} else {
				throw new UserError(this, 101, "CostEvaluator", label.getName());
			}
		} else {
			throw new UserError(this, 105);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_KEEP_EXAMPLE_SET, "Indicates if the example set should be kept.", false);
		type.setHidden(true);
		types.add(type);
		types.add(new ParameterTypeMatrix(PARAMETER_COST_MATRIX, "The matrix of missclassification costs. Columns and Rows in order of internal mapping.", "Cost Matrix", "Predicted Class", "True Class", true, false));
		return types;
	}
}
