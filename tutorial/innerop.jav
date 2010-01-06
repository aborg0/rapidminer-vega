[...]
// use first inner operator for learning on training set
Learner learner = (Learner)getOperator(0);
IOContainer container = 
  learner.apply(getInput().append(new IOObject[] {trainingSet}));

// apply model on test set
ModelApplier applier = (ModelApplier)getOperator(1);
container = 
  applier.apply(container.append(new IOObject[] {testSet}));

// retrieve the example set with predictions
ExampleSet withPredictions = 
  container.get(ExampleSet.class);
[...]
