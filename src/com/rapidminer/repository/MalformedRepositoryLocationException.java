package com.rapidminer.repository;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;

/**
 * 
 * @author Simon Fischer
 *
 */
public class MalformedRepositoryLocationException extends OperatorException {

	private static final long serialVersionUID = 1L;

	public MalformedRepositoryLocationException(String message) {
		super(message);
	}

	public UserError makeUserError(Operator operator) {
		return new UserError(operator, this, 319, this.getMessage());
	}
}
