import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.InputDescription;

...

/** Change the default behavior for input handling. */
public InputDescription getInputDescription(Class cls) {
    // returns a changed input description for example sets
    if (ExampleSet.class.isAssignableFrom(cls)) {
	// consume default: false, create parameter: true
        return new InputDescription(cls, false, true);
    } else {
        // other input types should be handled by super class
	return super.getInputDescription(cls);
    }
}

...