import com.rapidminer.tools.OperatorService;
import com.rapidminer.RapidMiner;
import com.rapidminer.Process;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import java.io.IOException;

public class ProcessCreator {

    public static Process createProcess() {
	try {
        // invoke init before using the OperatorService
	    RapidMiner.init();
	} catch (IOException e) { e.printStackTrace(); }

	// create process
	Process process = new Process();
	try {
	    // create operator
	    Operator inputOperator = 
	        OperatorService.createOperator(ExampleSetGenerator.class);
	        
	    // set parameters
	    inputOperator.setParameter("target_function", "sum classification");
	     
	    // add operator to process
	    process.getRootOperator().addOperator(inputOperator);

	    // add other operators and set parameters
	    // [...]
	} catch (Exception e) { e.printStackTrace(); }
	return process;
    }

    public static void main(String[] argv) {
	// create process
	Process process = createProcess();
	// print process setup
	System.out.println(process.getRootOperator().createProcessTree(0));

	try {
	    // perform process
	    process.run();
	    // to run the process with input created by your application use
        // process.run(new IOContainer(new IOObject[] { ... your objects ... });
	} catch (OperatorException e) { e.printStackTrace(); }
    }
}
