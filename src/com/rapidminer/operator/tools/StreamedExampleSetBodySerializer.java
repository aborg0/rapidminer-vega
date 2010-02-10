package com.rapidminer.operator.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.rapidminer.example.ExampleSet;

/**
 * 
 * @author Simon Fischer
 *
 */
public class StreamedExampleSetBodySerializer implements BodySerializer {
	private int version;
	protected StreamedExampleSetBodySerializer(int version) {
		this.version = version;
	}
	@Override
	public Object deserialize(InputStream in) throws IOException {
		return new ExampleSetToStream(version).read(in);
	}
	@Override
	public void serialize(Object object, OutputStream out) throws IOException {
		if (object instanceof ExampleSet) {
			new ExampleSetToStream(version).write((ExampleSet) object, out);
		} else {
			throw new IOException("Serialization type "+SerializationType.STREAMED_EXAMPLE_SET_DENSE+" only available for ExampleSets.");
		}
	}
	public int getVersion() {
		return version;
	}	
}
