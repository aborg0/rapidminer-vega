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
package com.rapidminer.operator.clustering.clusterer;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.clustering.CentroidClusterModel;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.numerical.EuclideanDistance;

/**
 * This operator represents an implementation of k-means. This operator will create a cluster attribute if not present
 * yet.
 * 
 * @author Sebastian Land
 */
public class KMeans extends RMAbstractClusterer implements CapabilityProvider {

	/** The parameter name for &quot;the maximal number of clusters&quot; */
	public static final String PARAMETER_K = "k";

	/**
	 * The parameter name for &quot;the maximal number of runs of the k method with random initialization that are
	 * performed&quot;
	 */
	public static final String PARAMETER_MAX_RUNS = "max_runs";

	/** The parameter name for &quot;the maximal number of iterations performed for one run of the k method&quot; */
	public static final String PARAMETER_MAX_OPTIMIZATION_STEPS = "max_optimization_steps";

	public KMeans(OperatorDescription description) {
		super(description);
		
		getExampleSetInputPort().addPrecondition(new CapabilityPrecondition(this, getExampleSetInputPort()));
	}

	@Override
	public ClusterModel generateClusterModel(ExampleSet exampleSet) throws OperatorException {
		int k = getParameterAsInt(PARAMETER_K);
		int maxOptimizationSteps = getParameterAsInt(PARAMETER_MAX_OPTIMIZATION_STEPS);
		int maxRuns = getParameterAsInt(PARAMETER_MAX_RUNS);
		DistanceMeasure measure = new EuclideanDistance();
		measure.init(exampleSet);

		// checking and creating ids if necessary
		Tools.checkAndCreateIds(exampleSet);

		// additional checks
		Tools.onlyNonMissingValues(exampleSet, "KMeans");
		if (exampleSet.size() < k) {
			throw new UserError(this, 142, k);
		}

		// extracting attribute names
		Attributes attributes = exampleSet.getAttributes();
		ArrayList<String> attributeNames = new ArrayList<String>(attributes.size());
		for (Attribute attribute : attributes)
			attributeNames.add(attribute.getName());

		RandomGenerator generator = RandomGenerator.getRandomGenerator(this);
		double minimalIntraClusterDistance = Double.POSITIVE_INFINITY;
		CentroidClusterModel bestModel = null;
		int[] bestAssignments = null;
		for (int iter = 0; iter < maxRuns; iter++) {
			checkForStop();
			CentroidClusterModel model = new CentroidClusterModel(exampleSet, k, attributeNames, measure, getParameterAsBoolean(RMAbstractClusterer.PARAMETER_ADD_AS_LABEL), getParameterAsBoolean(RMAbstractClusterer.PARAMETER_REMOVE_UNLABELED));
			// init centroids by assigning one single, unique example!
			int i = 0;
			for (Integer index : generator.nextIntSetWithRange(0, exampleSet.size(), k)) {
				model.assignExample(i, getAsDoubleArray(exampleSet.getExample(index), attributes));
				i++;
			}
			model.finishAssign();

			// run optimization steps
			int[] centroidAssignments = new int[exampleSet.size()];
			boolean stable = false;
			for (int step = 0; (step < maxOptimizationSteps) && !stable; step++) {
				// assign examles to new centroids
				i = 0;
				for (Example example : exampleSet) {
					double[] exampleValues = getAsDoubleArray(example, attributes);
					double nearestDistance = measure.calculateDistance(model.getCentroidCoordinates(0), exampleValues);
					int nearestIndex = 0;
					for (int centroidIndex = 1; centroidIndex < k; centroidIndex++) {
						double distance = measure.calculateDistance(model.getCentroidCoordinates(centroidIndex), exampleValues);
						if (distance < nearestDistance) {
							nearestDistance = distance;
							nearestIndex = centroidIndex;
						}
					}
					centroidAssignments[i] = nearestIndex;
					model.getCentroid(nearestIndex).assignExample(exampleValues);
					i++;
				}

				// finishing assignment
				stable = model.finishAssign();
			}
			// assessing quality of this model
			double distanceSum = 0;
			i = 0;
			for (Example example : exampleSet) {
				double distance = measure.calculateDistance(model.getCentroidCoordinates(centroidAssignments[i]), getAsDoubleArray(example, attributes));
				distanceSum += distance * distance;
				i++;
			}
			if (distanceSum < minimalIntraClusterDistance) {
				bestModel = model;
				minimalIntraClusterDistance = distanceSum;
				bestAssignments = centroidAssignments;
			}
		}
		bestModel.setClusterAssignments(bestAssignments, exampleSet);

		if (addsClusterAttribute()) {
			Attribute cluster = AttributeFactory.createAttribute("cluster", Ontology.NOMINAL);
			exampleSet.getExampleTable().addAttribute(cluster);
			exampleSet.getAttributes().setCluster(cluster);
			int i = 0;
			for (Example example : exampleSet) {
				example.setValue(cluster, "cluster_" + bestAssignments[i]);
				i++;
			}
		}

		return bestModel;
	}

	private double[] getAsDoubleArray(Example example, Attributes attributes) {
		double[] values = new double[attributes.size()];
		int i = 0;
		for (Attribute attribute : attributes) {
			values[i] = example.getValue(attribute);
			i++;
		}
		return values;
	}

	@Override
	public Class<? extends ClusterModel> getClusterModelClass() {
		return CentroidClusterModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
		case BINOMINAL_ATTRIBUTES:
		case POLYNOMINAL_ATTRIBUTES:
			return false;
		default:
			return true;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_K, "The number of clusters which should be detected.", 2, Integer.MAX_VALUE, 2, false));
		types.add(new ParameterTypeInt(PARAMETER_MAX_RUNS, "The maximal number of runs of k-Means with random initialization that are performed.", 1, Integer.MAX_VALUE, 10, false));
		types.add(new ParameterTypeInt(PARAMETER_MAX_OPTIMIZATION_STEPS, "The maximal number of iterations performed for one run of k-Means.", 1, Integer.MAX_VALUE, 100, false));
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}
}
