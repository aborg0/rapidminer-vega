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
package com.rapidminer.operator.features.transformation;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;

/**
 * A dimensionality reduction method based on Singular Value Decomposition.
 * 
 * @author Michael Wurst, Ingo Mierswa, Sebastian Land
 * 
 */
public class SVDReduction extends JamaDimensionalityReduction {

	public SVDReduction(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Matrix callMatrixMethod(ExampleSet es, int dimensions, Matrix in) {
		SingularValueDecomposition svd = new SingularValueDecomposition(in);
		Matrix u = svd.getU().getMatrix(0, es.size() - 1, 0, dimensions - 1);
		return u;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
		case NUMERICAL_ATTRIBUTES:
		case NO_LABEL:
			return true;
		default:
			return false;
		}
	}
}
