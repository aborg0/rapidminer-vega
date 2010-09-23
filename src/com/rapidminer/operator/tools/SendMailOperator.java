package com.rapidminer.operator.tools;

import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.tools.MailUtilities;

/**
 * 
 * @author Simon Fischer
 *
 */
public class SendMailOperator extends Operator {

	private DummyPortPairExtender through = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());
	
	public static final String PARAMETER_TO = "to";
	public static final String PARAMETER_SUBJECT = "subject";
	public static final String PARAMETER_BODY = "body";
	
	
	public SendMailOperator(OperatorDescription description) {
		super(description);
		through.start();
		getTransformer().addRule(through.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		String to = getParameterAsString(PARAMETER_TO);
		String subject = getParameterAsString(PARAMETER_SUBJECT);
		String body = getParameterAsString(PARAMETER_BODY);		
		MailUtilities.sendEmail(to, subject, body);
		through.passDataThrough();
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		final List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_TO, "Receiver of the email.", false, false));
		types.add(new ParameterTypeString(PARAMETER_SUBJECT, "Subject the email.", false, false));
		final ParameterTypeText type = new ParameterTypeText(PARAMETER_BODY, "Body of the email.", TextType.PLAIN, false);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
