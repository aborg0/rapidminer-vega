package com.rapidminer.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** When started for the first time, listens on a given socket.
 *  If started for the second time, contacts this socket and 
 *  passes command line options to this socket.
 * 
 * @author Simon Fischer
 *
 */
public class LaunchListener {

	private static final Logger LOGGER = Logger.getLogger(LaunchListener.class.getName());

	private static final LaunchListener INSTANCE = new LaunchListener();

	private LaunchListener() {		
	}
	
	private File getSocketFile() {
		File homeDir = new File(System.getProperty("user.home"));
		File userHomeDir = new File(homeDir, ".RapidMiner5");
		return new File(userHomeDir, "socket");
	}
	
	public static LaunchListener getInstance() {
		return INSTANCE;
	}

	private void installListener() throws IOException {
		ServerSocket serverSocket = new ServerSocket(0); // 0 = let system assign port
		int port = serverSocket.getLocalPort();
		final File socketFile = getSocketFile();
		LOGGER.info("Listening for other instances on port "+port+". Writing "+socketFile);
		PrintStream socketOut = new PrintStream(socketFile);
		socketOut.println(""+port);
		socketOut.close();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				LOGGER.config("Deleting "+socketFile);
				socketFile.delete();				
			}
		});
		
		while (true) {
			Socket client = serverSocket.accept();
			// We don't spawn another thread here. Assume no malicious client.
			talkToSecondClient(client);
		}
	}

	private void talkToSecondClient(Socket client) {
		try {
			LOGGER.info("Second client launched.");
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			PrintStream out = new PrintStream(client.getOutputStream());
			out.println("Hi. I am RapidMiner.");
			String line;
			while ((line = in.readLine()) != null) {
				LOGGER.info("Other client says: "+line);
				out.println("You said: "+line);
			}
			LOGGER.info("Other client terminated.");
			client.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to talk to client: "+e, e);
		}
	}

	private Socket getOtherInstance() {
		File socketFile = getSocketFile();
		if (!socketFile.exists()) {
			LOGGER.config("Socket file "+socketFile+" does not exist. Assuming I am the first instance.");
			return null;
		}
		int port;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(socketFile));
			String portStr = in.readLine();
			port = Integer.parseInt(portStr);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to read socket file '"+socketFile+"': "+ e, e);
			return null;
		} finally {
			try {
				in.close();
			} catch (IOException e) { }
		}
		LOGGER.config("Checking for running instance on port "+port+".");
		try {
			return new Socket("localhost", port);
		} catch (UnknownHostException e) {
			LOGGER.config("Name localhost cannot be resolved. Assuming we are the first instance.");
			return null;
		} catch (IOException e) {
			LOGGER.config("Got exception "+e+". Assuming we are the first instance.");
			return null;
		}
	}
	
	private boolean isOtherInstanceUp() {
		final Socket other = getOtherInstance();
		if (other != null) {
			try {
				other.close();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Failed to close connection: "+e, e);
			}
			return true;
		} else {
			return false;
		}
	}
	
	public static void main(String[] args) throws IOException {
		if (!getInstance().isOtherInstanceUp()) {
			getInstance().installListener();
		} else {
			LOGGER.info("Other client already up. Exiting.");
		}
	}
}
