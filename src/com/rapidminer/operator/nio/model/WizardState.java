package com.rapidminer.operator.nio.model;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.ImportWizardUtils;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;

/** The complete state of a data import wizard. Steps of the wizard communicate through this interface.
 * 
 * @author Simon Fischer
 *
 */
public class WizardState {
	
	private final DataResultSetTranslator translator;
	private final DataResultSetTranslationConfiguration config;	
	private final DataResultSetFactory dataResultSetFactory;
	private final AbstractDataResultSetReader operator;
	private final int maxRows = ImportWizardUtils.getPreviewLength();
	
	public WizardState(AbstractDataResultSetReader operator, DataResultSetFactory dataResultSetFactory) {
		super();
		this.config = new DataResultSetTranslationConfiguration(operator);
		this.translator = new DataResultSetTranslator(operator);
		this.operator = operator;
		this.dataResultSetFactory = dataResultSetFactory;
	}
	
	public DataResultSetTranslator getTranslator() {
		return translator;
	}	
	
	public DataResultSetTranslationConfiguration getTranslationConfiguration() {
		return config;
	}
	
	public DataResultSetFactory getDataResultSetFactory() {
		return dataResultSetFactory;
	}	

	public ExampleSet readNow(DataResultSet dataResultSet, boolean previewOnly, ProgressListener progressListener) throws OperatorException {
		LogService.getRoot().info("Reading example set...");
		ExampleSet cachedExampleSet = getTranslator().read(dataResultSet, getTranslationConfiguration(), previewOnly, progressListener);
		return cachedExampleSet;
	}

	public int getNumberOfPreviewRows() {		
		return maxRows;
	}

	public AbstractDataResultSetReader getOperator() {
		return operator;
	}
}
