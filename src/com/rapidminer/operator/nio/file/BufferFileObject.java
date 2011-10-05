package com.rapidminer.operator.nio.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;

import com.rapidminer.operator.OperatorException;
/**
 * Simple implementation of a {@link FileObject} backed by a {@link Buffer}.
 * 
 * @author Nils-Christian W�hler
 *
 */
public class BufferFileObject extends FileObject {

	private static final long serialVersionUID = 1L;

	private byte[] buffer;
	private File file = null;
	
	public BufferFileObject(byte[] buffer) {
		super();
		this.buffer = buffer;
	}

	@Override
	public ByteArrayInputStream openStream() throws OperatorException {
		return new ByteArrayInputStream(buffer);
	}

	@Override
	public File getFile() throws OperatorException {
		if(file==null){
			try {
				file = File.createTempFile("rm_file_", ".dump");
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(this.buffer);
				fos.close();
				file.deleteOnExit();
			} catch (IOException e) {
				throw new OperatorException("303", e, file, e.getMessage());
			}
			
			return file;
		} else {
			return file;
		}

	}
	
	@Override
	public String toString() {
		return (file != null) ? ("Buffered file stored in temporary file: " + file.getAbsolutePath()) : ("Memory buffered file");
	}
	
	@Override
	protected void finalize() throws Throwable {
		if(file!=null){
			file.delete();
		}
		super.finalize();
	}


}
