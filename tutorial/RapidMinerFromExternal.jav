public static IOContainer createInput() {
    // create a wrapper that implements the ExampleSet interface and
    // encapsulates your data
    // ...
    return new IOContainer(IOObject[] { myExampleSet });
}

public static void main(String[] argv) throws Exception {
    // MUST BE INVOKED BEFORE ANYTHING ELSE !!!
    RapidMiner.init();

    // create the process from the command line argument file
    Process process = new Process(new File(argv[0]));

    // create some input from your application, e.g. an example set
    IOContainer input = createInput();
    
    // run the process on the input
    process.run(input);
}
