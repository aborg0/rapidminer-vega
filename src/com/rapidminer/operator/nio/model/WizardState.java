package com.rapidminer.operator.nio.model;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;

/** The complete state of a data import wizard. Steps of the wizard communicate through this interface.
 * 
 * @author Simon Fischer
 *
 */
public class WizardState {
	
	private DataResultSetTranslator translator = null;
	private DataResultSetTranslationConfiguration config;
	
	private DataResultSetFactory dataResultSetFactory;

	private int maxRows = 100;
	private ExampleSet cachedExampleSet;
	
	public WizardState(DataResultSetFactory dataResultSetFactory) {
		super();
		this.dataResultSetFactory = dataResultSetFactory;
		try {
			maxRows = Integer.parseInt(RapidMiner.getRapidMinerPropertyValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_MAX_TEST_ROWS));
		} catch (NumberFormatException e) {
			maxRows = 100;
		}

	}
	
	public DataResultSetTranslator getTranslator() {
		return translator;
	}
	public void setTranslator(DataResultSetTranslator translator) {
		this.translator = translator;
	}
	
	public DataResultSetTranslationConfiguration getTranslationConfiguration() {
		return config;
	}
	public void setTranslationConfiguration(DataResultSetTranslationConfiguration config) {
		this.config = config;
	}
	
	public DataResultSetFactory getDataResultSetFactory() {
		return dataResultSetFactory;
	}
	public void setDataResultSetFactory(DataResultSetFactory dataResultSetFactory) {
		this.dataResultSetFactory = dataResultSetFactory;
	}

	public ExampleSet readNow(boolean previewOnly, ProgressListener progressListener) throws OperatorException {
		cachedExampleSet = getTranslator().read(getTranslationConfiguration(), 
				previewOnly ? maxRows : 0,
				progressListener);
		LogService.getRoot().info("Reading example set...");
		return cachedExampleSet;
	}

	public int getNumberOfPreviewRows() {		
		return maxRows;
	}

	public ExampleSet getCachedExampleSet() {
		return cachedExampleSet;
	}
}
