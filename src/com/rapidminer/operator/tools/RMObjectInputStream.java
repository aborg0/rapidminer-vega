package com.rapidminer.operator.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import com.rapidminer.tools.plugin.Plugin;

/** Uses {@link Plugin#getMajorClassLoader()} to load classes.
 * 
 * @author Simon Fischer
 *
 */
public class RMObjectInputStream extends ObjectInputStream {
	private ClassLoader classLoader = Plugin.getMajorClassLoader();

	public RMObjectInputStream(InputStream in) throws IOException {
		super(in);
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc)
			throws IOException, ClassNotFoundException {
		return Class.forName(desc.getName(), true, classLoader);
	}
}
