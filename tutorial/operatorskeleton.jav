package my.new.operators;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.IOObject;
import com.rapidminer.parameter.ParameterType;

import java.util.List;

public class OperatorSkeleton extends Operator {

    /** Must pass the given object to the superclass. */
    public OperatorSkeleton(OperatorDescription description) {
        super(description);
    }

    /** Perform the operators action here. */
    public IOObject[] apply() throws OperatorException {
	// describe the core function of this operator
	return new IOObject[0];
    }

    /** Add your parameters to the list. */
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
	// add your parameter types here
	return types;
    }

    /** Return the required input classes. */
    public Class[] getInputClasses() { return new Class[0]; }

    /** Return the delivered output classes. */
    public Class[] getOutputClasses() { return new Class[0]; }
}
