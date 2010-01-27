package com.rapidminer.repository.gui.process;

/** Required to wrap around string for instanceof check in tree model.
 * 
 * @author Simon Fischer
 *
 */
public class ExceptionWrapper {

	private String exception;
	
	public ExceptionWrapper(String exception) {
		setException(exception);
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	public String getException() {
		return exception;
	}

	@Override
	public String toString() {
		return exception;
	}
}
