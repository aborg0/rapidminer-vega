public static void main(String[] args) { 
    try {
        RapidMiner.init();

	// learn
	Operator exampleSource = 
	    OperatorService.createOperator(ExampleSource.class);
	exampleSource.setParameter("attributes", 
				   "/path/to/your/training_data.xml");
	IOContainer container = exampleSource.apply(new IOContainer()); 
	ExampleSet exampleSet = container.get(ExampleSet.class); 
 
    // here the string based creation must be used since the J48 operator
    // do not have an own class (derived from the Weka library).
	Learner learner = (Learner)OperatorService.createOperator("J48"); 
	Model model = learner.learn(exampleSet); 
 
	// loading the test set (plus adding the model to result container)
	Operator testSource = 
	    OperatorService.createOperator(ExampleSource.class);
	testSource.setParameter("attributes", "/path/to/your/test_data.xml");
	container = testSource.apply(new IOContainer());
	container = container.append(model);

	// applying the model
    Operator modelApp = 
        OperatorService.createOperator(ModelApplier.class); 
	container = modelApp.apply(container);
	    
	// print results
	ExampleSet resultSet = container.get(ExampleSet.class);
        Attribute predictedLabel = resultSet.getPredictedLabel();
	ExampleReader reader = resultSet.getExampleReader();
	while (reader.hasNext()) {
            System.out.println(reader.next().getValueAsString(predictedLabel));
	}
    } catch (IOException e) { 
	System.err.println("Cannot initialize RapidMiner:" + e.getMessage()); 
    } catch (OperatorCreationException e) { 
	System.err.println("Cannot create operator:" + e.getMessage()); 
    } catch (OperatorException e) { 
	System.err.println("Cannot create model: " + e.getMessage()); 
    } 
}
