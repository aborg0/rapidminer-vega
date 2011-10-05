package com.rapidminer.operator.nio.file;

import java.io.File;
import java.io.InputStream;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ResultObjectAdapter;

/**
 * 
 * This class represents buffers, files or streams that can be parsed by Operators.
 * 
 * @author Nils-Christian Wöhler
 *
 * 
 *
 */
public abstract class FileObject extends ResultObjectAdapter {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Open Stream to read data in this Object.
	 * @throws OperatorException 
	 * 
	 */
	public abstract InputStream openStream() throws OperatorException;
	
	/**
	 * Returns the data as a file. Maybe slow if underlaying implementation needs to copy the data into the file first.
	 * 
	 */
	public abstract File getFile() throws OperatorException ;

	@Override
	public String getName() {
		return "File";
	}
}
