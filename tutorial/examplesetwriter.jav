package my.new.operators;

import com.rapidminer.example.*;
import com.rapidminer.operator.*;
import com.rapidminer.parameter.*;
import java.io.*;
import java.util.List;

public class ExampleSetWriter extends Operator {

  public ExampleSetWriter(OperatorDescription description) {
    super(description);
  }

  public IOObject[] apply() throws OperatorException {
    File file = getParameterAsFile("example_set_file");
    ExampleSet eSet = getInput(ExampleSet.class);
    try {
      PrintWriter out = new PrintWriter(new FileWriter(file));
      for (Example example : eSet) {
         out.println(example); 
      }
      out.close();
    } catch (IOException e) {
      throw new UserError(this, 303, file, e.getMessage());
    }
    return new IOObject[] { eSet };
  }

  public List<ParameterType> getParameterTypes() {
    List<ParameterType> types = super.getParameterTypes();
    types.add(new ParameterTypeFile("example_set_file", 
				    "The file for the examples.",
				    "txt", // default file extension
				    false)); // non-optional
    return types;
  }

  public Class[] getInputClasses() { 
    return new Class[] { ExampleSet.class }; 
  }

  public Class[] getOutputClasses() { 
    return new Class[] { ExampleSet.class }; 
  }
}

