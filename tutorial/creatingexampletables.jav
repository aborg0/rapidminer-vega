import com.rapidminer.example.*;
import com.rapidminer.example.table.*;
import com.rapidminer.example.set.*;
import com.rapidminer.tools.Ontology;
import java.util.*;

public class CreatingExampleTables {

  public static void main(String[] argv) {
    // create attribute list
    List<Attribute> attributes = new LinkedList<Attribute>();
    for (int a = 0; a < getMyNumOfAttributes(); a++) {
      attributes.add(AttributeFactory.createAttribute("att" + a, 
                                                      Ontology.REAL));
    }
    Attribute label = AttributeFactory.createAttribute("label", 
                                                       Ontology.NOMINAL));
    attributes.add(label);
		
    // create table
    MemoryExampleTable table = new MemoryExampleTable(attributes);
		
    // fill table (here: only real values)
    for (int d = 0; d < getMyNumOfDataRows(); d++) {
      double[] data = new double[attributes.size()];
      for (int a = 0; a < getMyNumOfAttributes(); a++) {	
        // fill with proper data here
        data[a] = getMyValue(d, a);
      }
			
      // maps the nominal classification to a double value
      data[data.length - 1] = 
          label.getMapping().mapString(getMyClassification(d));
          
      // add data row
      table.addDataRow(new DoubleArrayDataRow(data));
    }
		
    // create example set
    ExampleSet exampleSet = table.createExampleSet(label);
  }
}