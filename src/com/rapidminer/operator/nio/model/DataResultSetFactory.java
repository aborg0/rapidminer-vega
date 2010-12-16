package com.rapidminer.operator.nio.model;

import javax.swing.table.TableModel;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.tools.ProgressListener;

/**
 * 
 * @author Simon Fischer
 *
 */
public interface DataResultSetFactory {

	/** Creates a result set. Make sure to call {@link #close()} after using this method. */
	public DataResultSet makeDataResultSet(Operator operator) throws OperatorException;

	public TableModel makePreviewTableModel(ProgressListener listener) throws OperatorException, ParseException;

	/** Returns the human readable name of the resource read (most often, this will be a file or URL). */
	public String getResourceName();

	/** Makes initial meta data. Only the number of rows should be filled in here. All other information
	 *  will later be added by {@link DataResultSetTranslationConfiguration}*/
	public ExampleSetMetaData makeMetaData();

	/** Sets the configuration parameters in the given reader operator. */
	public void setParameters(AbstractDataResultSetReader reader);

	/** Closes all resources associated with this factory. */
	public void close();
}
