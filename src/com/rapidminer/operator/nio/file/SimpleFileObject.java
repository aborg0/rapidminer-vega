package com.rapidminer.operator.nio.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.rapidminer.operator.OperatorException;
/**
 * Simple implementation of a {@link FileObject} backed by a {@link File}.
 * 
 * @author Nils-Christian Wöhler
 *
 */
public class SimpleFileObject extends FileObject {

	private static final long serialVersionUID = 1L;

	private File file;
	
	
	public SimpleFileObject(File file) {
		super();
		this.file = file;
	}

	@Override
	public InputStream openStream() throws OperatorException {
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new OperatorException("301", e, file);
		}
	}

	@Override
	public File getFile() {
		return file;
	}
	
	@Override
	public String toString() {
		return "File: "+getFile().getAbsolutePath();
	}


}
