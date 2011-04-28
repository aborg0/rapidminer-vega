package com.rapidminer.test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.remote.RemoteRepository;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;

/**
 * <p>The test context (singleton) is in charge for initializing RapidMiner and determining a
 * test repository.</p>
 * 
 * <p>You can specify the test repository by setting the following system properties for this repository:
 * 
 *  <ul>
 *  	<li>rapidminer.test.repository.url</li>
 *      <li>rapidminer.test.repository.location</li>
 *  	<li>rapidminer.test.repository.user</li>
 *  	<li>rapidminer.test.repository.password</li>
 *  <ul>
 *  </p>
 *  
 *  <p>Alternatively a file 'test.properties' with this properties can be saved in the home directory
 *  of RapidMiner.</p>
 *  
 *  <p>The alias for the repository will be 'junit'.</p>
 * 
 * @author Marcin Skirzynski
 *
 */
public class TestContext {

	/**
	 * Singleton instance
	 */
	private static volatile TestContext INSTANCE = null;
	
	/**
	 * File name for the properties
	 */
	public static String PROPERTY_TEST_FILE = 					"test.properties";
    
	/**
	 * Property name for the URL to the test repository
	 */
    public static String PROPERTY_TEST_REPOSITORY_URL = 		"rapidminer.test.repository.url";
    
    /**
     *Property name for the location to the test repository
     */
    public static String PROPERTY_TEST_REPOSITORY_LOCATION = 	"rapidminer.test.repository.location";
    
    /**
     * Property name for the user name for the test repository 
     */
    public static String PROPERTY_TEST_REPOSITORY_USER = 		"rapidminer.test.repository.user";
    
    /**
     * Property name for the password for the test repository 
     */
    public static String PROPERTY_TEST_REPOSITORY_PASSWORD = 	"rapidminer.test.repository.password";
    
    /**
     * Displayed repostiory alias.
     */
    public static String REPOSITORY_ALIAS = 					"junit";
	
	private boolean initialized = false;
	
	private boolean repositoryPresent = false;
	
	private Repository repository;
	
	private RepositoryLocation repositoryLocation;
	
	/**
	 * Does not allow external instantiation
	 */
	private TestContext() {}
	
	/**
	 * Returns the singleton instance of the test context
	 * 
	 * @return	the test context
	 */
    public static TestContext get() {
        if (INSTANCE == null) {
            synchronized(TestContext.class) {
                if (INSTANCE == null)
                	INSTANCE = new TestContext(); 
            }
        }
        return INSTANCE;
    }
	
    /**
     * <p>Initializes RapidMiner and tries to fetch the information for the test repository.</p>
     * 
	 * <p>You can specify the test repository by setting the following system properties for this repository:
	 * 
	 *  <ul>
	 *  	<li>rapidminer.test.repository.url</li>
	 *      <li>rapidminer.test.repository.location</li>
	 *  	<li>rapidminer.test.repository.user</li>
	 *  	<li>rapidminer.test.repository.password</li>
	 *  <ul>
	 *  </p>
	 *  
	 *  <p>Alternatively a file 'test.properties' with this properties can be saved in the home directory
	 *  of RapidMiner.</p>
	 *  
	 *  <p>The alias for the repository will be 'junit'.</p>
     */
	public void initRapidMiner() {
		
		 if (!isInitialized()) {
	            File testConfigFile = FileSystemService.getUserConfigFile(PROPERTY_TEST_FILE);
	            
	            Properties properties = new Properties();
	            if (testConfigFile.exists()) {
	                FileInputStream in;
	                try {
	                    in = new FileInputStream(testConfigFile);
	                    properties.load(in);
	                    in.close();
	                } catch (Exception e) {
	                    throw new RuntimeException("Failed to read " + testConfigFile,e);
	                }
	            } else {
	            	properties = System.getProperties();
	            }

	            String repositoryUrl = properties.getProperty(PROPERTY_TEST_REPOSITORY_URL);
	            String repositoryLocation = properties.getProperty(PROPERTY_TEST_REPOSITORY_LOCATION);
	            String repositoryUser= properties.getProperty(PROPERTY_TEST_REPOSITORY_USER);
	            String repositoryPassword = properties.getProperty(PROPERTY_TEST_REPOSITORY_PASSWORD);
	            

	            RapidMiner.setExecutionMode(ExecutionMode.TEST);
	            RapidMiner.init();

	            try {
	            	if (repositoryUrl!=null&&repositoryLocation!=null&&repositoryUser!=null&&repositoryPassword!=null) {
	            		setRepository(new RemoteRepository(new URL(repositoryUrl), REPOSITORY_ALIAS, repositoryUser, repositoryPassword.toCharArray(), false));
	            		setRepositoryLocation(new RepositoryLocation(repositoryLocation));
	            		RepositoryManager.getInstance(null).addRepository(getRepository());
	            		setRepositoryPresent(true);
	            	} else {
	            		LogService.getRoot().log(Level.WARNING,
	            				"In order to run repository tests, please define system property "
	            				+PROPERTY_TEST_REPOSITORY_URL+", "
	            				+PROPERTY_TEST_REPOSITORY_LOCATION+", "
	            				+PROPERTY_TEST_REPOSITORY_USER+" and "
	            				+PROPERTY_TEST_REPOSITORY_PASSWORD+
	            				" in your run configuration or create a property file called "+PROPERTY_TEST_FILE+" with this values which point to the test repository.");
	            	}
	            } catch (Exception e) {
	            	setRepositoryPresent(false);
	                throw new RuntimeException("Failed to intialize test repository", e);
	            }

	            setInitialized(true);
	        }
		
	}

	/**
	 * @param initialized the initialized to set
	 */
	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/**
	 * @return the initialized
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * @param repository the repository to set
	 */
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	/**
	 * @return the repository
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @param repositoryLocation the repositoryLocation to set
	 */
	public void setRepositoryLocation(RepositoryLocation repositoryLocation) {
		this.repositoryLocation = repositoryLocation;
	}

	/**
	 * @return the repositoryLocation
	 */
	public RepositoryLocation getRepositoryLocation() {
		return repositoryLocation;
	}

	/**
	 * @param repositoryPresent the repositoryPresent to set
	 */
	public void setRepositoryPresent(boolean repositoryPresent) {
		this.repositoryPresent = repositoryPresent;
	}

	/**
	 * @return the repositoryPresent
	 */
	public boolean isRepositoryPresent() {
		return repositoryPresent;
	}
	
	

}
