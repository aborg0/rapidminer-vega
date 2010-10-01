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
package com.rapidminer.operator.nio.model;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.io.AbstractDataReader.AttributeColumn;
import com.rapidminer.operator.preprocessing.filter.AbstractDateDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;

/**
 * This class uses DataResultSets to load data from file and then delivers the data as an example set.
 * 
 * @author Sebastian Land
 */
public abstract class AbstractDataResultSetReader extends AbstractExampleSource {

	/** Pseudo-annotation to be used for attribute names. */
	public static final String ANNOTATION_NAME = "Name";

	
	/**
	 * This parameter holds the hole information about the attribute columns. I.e. which attributes are defined, the
	 * names, what value type they have, whether the att. is selected,
	 */
	private static final String PARAMETER_META_DATA = "data_set_meta_data_information";

	/**
	 * Parameters being part of the list for PARAMETER_META_DATA
	 */
	public static final String PARAMETER_COLUMN_INDEX = "column_index";
	public static final String PARAMETER_COLUMN_META_DATA = "attribute_meta_data_information";
	public static final String PARAMETER_COLUMN_NAME = "attribute name";
	public static final String PARAMETER_COLUMN_SELECTED = "column_selected";
	public static final String PARAMETER_COLUMN_VALUE_TYPE = "attribute_value_type";
	public static final String PARAMETER_COLUMN_ROLE = "attribute_role";

	public static final String PARAMETER_DATE_FORMAT = "date_format";
	public static final String PARAMETER_TIME_ZONE = "time_zone";
	public static final String PARAMETER_LOCALE = "locale";
	/**
	 * The parameter name for &quot;Determines, how the data is represented internally.&quot;
	 */
	public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";
	public static final String PARAMETER_FIRST_ROW_AS_NAMES = "first_row_as_names";
	public static final String PARAMETER_ANNOTATIONS = "annotations";
	
	
	
	public AbstractDataResultSetReader(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		// loading data result set
		DataResultSet dataResultSet = getDataResultSet();

		// loading configuration
		DataResultSetTranslationConfiguration configuration = new DataResultSetTranslationConfiguration(this, dataResultSet);

		// now use translator to read, translate and return example set
		DataResultSetTranslator translator = new DataResultSetTranslator(this, dataResultSet);
		translator.guessValueTypes(configuration, dataResultSet, null);
		return translator.read(configuration, 0, null);
	}


	/**
	 * Must be implemented by subclasses to return the DataResultSet.
	 */
	protected abstract DataResultSet getDataResultSet() throws OperatorException;

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeBoolean(PARAMETER_FIRST_ROW_AS_NAMES, "Indicates if the first row should be used for the attribute names. If activated no annotations can be used.", true, true));

		ParameterType type = new ParameterTypeDateFormat(PARAMETER_DATE_FORMAT, "The parse format of the date values, for example \"yyyy/MM/dd\".", false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_TIME_ZONE, "The time zone used for the date objects if not specified in the date string itself.", Tools.getAllTimeZones(), Tools.getPreferredTimeZoneIndex());
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_LOCALE, "The used locale for date texts, for example \"Wed\" (English) in contrast to \"Mi\" (German).", AbstractDateDataProcessing.availableLocaleNames, AbstractDateDataProcessing.defaultLocale);
		types.add(type);

		
		
		List<String> annotations = new LinkedList<String>();
		annotations.add(ANNOTATION_NAME);
		annotations.addAll(Arrays.asList(Annotations.ALL_KEYS_ATTRIBUTE));
		type = new ParameterTypeList(PARAMETER_ANNOTATIONS, "Maps row numbers to annotation names.", //
				new ParameterTypeInt("row_number", "Row number which contains an annotation", 0, Integer.MAX_VALUE), //
				new ParameterTypeCategory("annotation", "Name of the annotation to assign this row.", annotations.toArray(new String[annotations.size()]), 0), true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_FIRST_ROW_AS_NAMES, false, false));
		types.add(type);

		
		type = new ParameterTypeList(PARAMETER_META_DATA, "The meta data information", //
				new ParameterTypeInt(PARAMETER_COLUMN_INDEX, "The column index", 0, Integer.MAX_VALUE), //
				new ParameterTypeTupel(PARAMETER_COLUMN_META_DATA, "The meta data definition of one column", // 
						new ParameterTypeString(PARAMETER_COLUMN_NAME, "Describes the attributes name."), //
						new ParameterTypeBoolean(PARAMETER_COLUMN_SELECTED, "Indicates if a column is selected", true), //
						new ParameterTypeCategory(PARAMETER_COLUMN_VALUE_TYPE, "Indicates the value type of an attribute", Ontology.VALUE_TYPE_NAMES, Ontology.NOMINAL), // 
						new ParameterTypeStringCategory(PARAMETER_COLUMN_ROLE, "Indicates the role of an attribute", Attributes.KNOWN_ATTRIBUTE_TYPES, AttributeColumn.REGULAR)), true);

		types.add(type);

		return types;
	}
}
