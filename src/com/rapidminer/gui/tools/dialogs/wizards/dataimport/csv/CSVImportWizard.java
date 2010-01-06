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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv;

import java.io.File;
import java.util.LinkedList;

import com.rapidminer.gui.tools.SimpleFileFilter;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.AttributeSelectionWizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizard;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.FileSelectionWizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.RepositoryLocationSelectionWizardStep;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.CSVDataReader;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.DateParser;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.StrictDecimalFormat;
import com.rapidminer.tools.io.Encoding;


/**
 * A wizard to import CSV files into the repository.
 * 
 * @author Tobias Malbrecht
 */
public class CSVImportWizard extends DataImportWizard {

	private static final long serialVersionUID = -4308448171060612833L;
	
	private File file = null;
	
	private CSVDataReader reader = null;
	
	private ExampleSetMetaData metaData = null;
	
	public CSVImportWizard(String i18nKey, Object ... i18nArgs) {
		this(i18nKey, (File) null, (RepositoryLocation) null, i18nArgs);
	}
	
	public CSVImportWizard(String i18nKey, final File preselectedFile, final RepositoryLocation preselectedLocation, Object ... i18nArgs) {
		super(i18nKey, i18nArgs);
		file = preselectedFile;
		try { 
			reader = OperatorService.createOperator(com.rapidminer.operator.io.CSVDataReader.class);
		} catch (OperatorCreationException e) {
			
		}
		if (preselectedFile == null) {
			addStep(new FileSelectionWizardStep(this, new SimpleFileFilter("CSV File (.csv)", ".csv")) {
				@Override
				protected boolean performLeavingAction() {
					file = getSelectedFile();
					return true;
				}
			});
		}
		addStep(new ParseFileWizardStep("specify_csv_parsing_options") {
			@Override
			protected boolean canGoBack() {
				return true;
			}

			@Override
			protected boolean canProceed() {
				return true;
			}
			
			@Override
			protected void settingsChanged() {
				reader.setParameter(Encoding.PARAMETER_ENCODING, getEncoding().displayName());
				reader.setParameter(CSVDataReader.PARAMETER_TRIM_LINES, Boolean.toString(trimLines()));
				reader.setParameter(CSVDataReader.PARAMETER_SKIP_COMMENTS, Boolean.toString(skipComments()));
				reader.setParameter(CSVDataReader.PARAMETER_COMMENT_CHARS, getCommentCharacters());
				reader.setParameter(CSVDataReader.PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES, Boolean.toString(getUseFirstRowAsColumnNames()));
				reader.setParameter(CSVDataReader.PARAMETER_USE_QUOTES, Boolean.toString(useQuotes()));
				reader.setParameter(CSVDataReader.PARAMETER_QUOTES_CHARACTER, Character.toString(getQuotesCharacter()));
				reader.setParameter(CSVDataReader.PARAMETER_COLUMN_SEPARATORS, getSplitExpression());
				reader.setCachePreview(true);
				try {
					metaData = (ExampleSetMetaData) reader.getGeneratedMetaData();
				} catch (OperatorException e) {
				}
				LinkedList<Object[]> data = reader.getPreview();
				setData(metaData, data);
			}
			
			@Override
			protected boolean performEnteringAction() {
				reader.setParameter(CSVDataReader.PARAMETER_FILE_NAME, file.getAbsolutePath());
				settingsChanged();
				return true;
			}
			
			@Override
			protected boolean performLeavingAction() {
				return true;
			}
		});
		addStep(new ParseValueTypesWizardStep("value_type_selection") {
			@Override
			protected boolean canGoBack() {
				return true;
			}

			@Override
			protected boolean canProceed() {
				return true;
			}
			
			protected void settingsChanged() {
				reader.setParameter(StrictDecimalFormat.PARAMETER_DECIMAL_CHARACTER, Character.toString(getDecimalPointCharacter()));
				reader.setParameter(StrictDecimalFormat.PARAMETER_GROUPED_DIGITS, Boolean.toString(this.groupDigits()));
				reader.setParameter(StrictDecimalFormat.PARAMETER_GROUPING_CHARACTER, Character.toString(getGroupingSeparator()));
				reader.setParameter(DateParser.PARAMETER_DATE_FORMAT, getDateFormat());
				reader.setCachePreview(true);
				try {
					metaData = (ExampleSetMetaData) reader.getGeneratedMetaData();
				} catch (OperatorException e) {
				}
				LinkedList<Object[]> data = reader.getPreview();
				setData(metaData, data);
			}

			@Override
			protected boolean performEnteringAction() {
				settingsChanged();
				return true;
			}
			
			@Override
			protected boolean performLeavingAction() {
				return true;
			}
		});
		addStep(new AttributeSelectionWizardStep("select_attributes") {
			@Override
			protected boolean canGoBack() {
				return true;
			}

			@Override
			protected boolean canProceed() {
				return true;
			}

			@Override
			protected boolean performEnteringAction() {
				setMetaData(metaData);
				return true;
			}
		});
		addStep(new RepositoryLocationSelectionWizardStep("select_repository_location", this, null, preselectedLocation != null ? preselectedLocation.getAbsoluteLocation() : null) {
			@Override
			protected boolean performLeavingAction() {
				return transferData(reader, metaData, getRepositoryLocation());
			}
		});
		layoutDefault();
	}
}
