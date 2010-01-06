package my.new.operators;

import com.rapidminer.operator.*;
import com.rapidminer.operator.performance.*;
import com.rapidminer.example.*;

public class MyOperatorChain extends OperatorChain {

  public MyOperatorChain(OperatorDescription description) {
    super(description);
  }

  public IOObject[] apply() throws OperatorException {
    ExampleSet exampleSet = getInput(ExampleSet.class);
    ExampleSet clone = null;
    PerformanceVector result = new PerformanceVector();
    for (int i = 0; i < getNumberOfOperators(); i++) {
      clone = (ExampleSet)exampleSet.clone();
      IOContainer input = getInput().append(new IOObject[] { clone });
      IOContainer applyResult = getOperator(i).apply(input);
      PerformanceVector vector = 
          applyResult.getInput(PerformanceVector.class);
      result.buildAverages(vector);
    }
    return new IOObject[] { result };
  }

  public Class[] getInputClasses() {
    return new Class[] { ExampleSet.class };
  }

  public Class[] getOutputClasses() {
    return new Class[] { PerformanceVector.class };
  }

  public InnerOperatorCondition getInnerOperatorCondition() {
    return new AllInnerOperatorCondition(new Class[] { ExampleSet.class},
                                         new Class[] { PerformanceVector.class} );
  }

  public int getMinNumberOfInnerOperators() { return 1; }
  public int getMaxNumberOfInnerOperators() { return Integer.MAX_VALUE; }
  public int getNumberOfSteps() { return super.getNumberOfChildrensSteps(); }
}

