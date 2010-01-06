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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.operator.Model;
/**
 * @author Simon Fischer
 */
public class ModelMetaData extends MetaData {
	private static final long serialVersionUID = 1L;

	private ExampleSetMetaData trainingSetMetaData;
	
	/** Clone constructor */
	protected ModelMetaData() {}
	
	public ModelMetaData(ExampleSetMetaData trainingSetMetaData) {
		this(Model.class, trainingSetMetaData);
	}

	public ModelMetaData(Class<? extends Model> mclass, ExampleSetMetaData trainingSetMetaData) {
		super(mclass);
		this.trainingSetMetaData = trainingSetMetaData;
	}

	@Override
	public String getDescription() {
		return super.getDescription(); 
	}

	public ExampleSetMetaData apply(ExampleSetMetaData emd) {
		return emd;
	}
	
	@Override
	public ModelMetaData clone() {
		ModelMetaData md = (ModelMetaData) super.clone();
		if (trainingSetMetaData != null)
			md.trainingSetMetaData = trainingSetMetaData.clone();
		return md;
	}
	
	public ExampleSetMetaData getTrainingSetMetaData() {
		return trainingSetMetaData;
	}

}
