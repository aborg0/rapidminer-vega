public MyOperator(OperatorDescription description) {
  // invoke super-constructor
  super(description);
  // add values for process logging
  addValue(new Value("number", "The current number.") {
      public double getValue() {
        return currentNumber;
      }
    });
  addValue(new Value("performance", "The best performance.") {
      public double getValue() {
        return bestPerformance;
      }
    });
}
