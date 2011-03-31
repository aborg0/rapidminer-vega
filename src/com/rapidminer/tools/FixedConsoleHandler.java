package com.rapidminer.tools;

import java.lang.reflect.Method;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.StreamHandler;

/** The regular {@link ConsoleHandler} fails to call super.close() which prevents
 *  {@link Formatter#getTail(java.util.logging.Handler)} to be written propertly. 
 *  We solve this by reflectively calling the private method {@link StreamHandler#flushAndClose}.
 *  
 * @author Simon Fischer
 *
 */
public class FixedConsoleHandler extends ConsoleHandler {

	@Override
	public void close() {
		super.close();
		
		Method flushAndClose;
		try {
			flushAndClose = StreamHandler.class.getDeclaredMethod("flushAndClose", new Class[0]);
			flushAndClose.setAccessible(true);
			flushAndClose.invoke(this);
		} catch (Exception e) {
			//e.printStackTrace();
		}				
	}
	
}
