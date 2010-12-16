package com.rapidminer.operator.nio;

import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizard;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.operator.nio.model.DataResultSetFactory;
import com.rapidminer.operator.nio.model.WizardState;
import com.rapidminer.repository.RepositoryLocation;

/** All new data import wizards should inherit from this class. It provides 
 *  the common steps (annotations, meta data, saving) for all import wizards.
 * 
 * @author Simon Fischer
 *
 */
public abstract class AbstractDataImportWizard extends DataImportWizard {

	private static final long serialVersionUID = 1L;
	
	private final WizardState state;
	private final AbstractDataResultSetReader reader;

	private RepositoryLocation preselectedLocation;
	
	public AbstractDataImportWizard(AbstractDataResultSetReader reader, RepositoryLocation preselectedLocation, String key, Object ... arguments) throws OperatorException {
		super(key, arguments);
		this.reader = reader;
		this.preselectedLocation = preselectedLocation;
		DataResultSetFactory factory = makeFactory(reader);
		state = new WizardState(reader, factory);
	}
	
	/** Creates a {@link DataResultSetFactory} for the {@link AbstractDataResultSetReader} given
	 *  in the constructor. */
	protected abstract DataResultSetFactory makeFactory(AbstractDataResultSetReader reader) throws OperatorException;

	protected void addCommonSteps() {
		addStep(new AnnotationDeclarationWizardStep(getState()));
		addStep(new MetaDataDeclarationWizardStep(getState()));
		if (getReader() == null) {
			addStep(new StoreDataWizardStep(this, getState(), (preselectedLocation != null) ? preselectedLocation.getAbsoluteLocation() : null));
		}
	}


	@Override
	public void cancel() {
		super.cancel();
		getState().getDataResultSetFactory().close();
	}
	
	public WizardState getState() {
		return state;
	}

	public AbstractDataResultSetReader getReader() {
		return reader;
	}
	
	@Override
	public void finish() {
		super.finish();
		if (reader != null) { // we are configuring an operator
			state.getTranslationConfiguration().setParameters(reader);
			state.getDataResultSetFactory().setParameters(reader);
			getState().getDataResultSetFactory().close();
		}
	}
}
