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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.RapidMiner;
import com.rapidminer.io.process.XMLTools;

/** When started for the first time, listens on a given socket.
 *  If started for the second time, contacts this socket and 
 *  passes command line options to this socket.
 * 
 * @author Simon Fischer
 *
 */
public class LaunchListener {

	public static interface RemoteControlHandler {

		boolean handleArguments(String[] args);

	}

	private static final String FAILED = "<failed/>";
	
	private static final String UNKNOWN_COMMAND = "<unknown-command/>";

	private static final String REJECTED = "<rejected/>";

	private static final String OK = "<ok/>";

	private static final String HELLO_MESSAGE = "<hi>I am RapidMiner. I understand a bit of XML.</hi>";

	private static final Logger LOGGER = Logger.getLogger(LaunchListener.class.getName());

	private static final LaunchListener INSTANCE = new LaunchListener();

	private RemoteControlHandler handler;

	private LaunchListener() {		
	}

	private File getSocketFile() {
		return ParameterService.getUserConfigFile("socket");
	}

	public static LaunchListener getInstance() {
		return INSTANCE;
	}

	private void installListener(RemoteControlHandler handler) throws IOException {
		ServerSocket serverSocket = new ServerSocket(0); // 0 = let system assign port
		int port = serverSocket.getLocalPort();
		final File socketFile = getSocketFile();
		LOGGER.info("Listening for other instances on port "+port+". Writing "+socketFile);
		PrintStream socketOut = new PrintStream(socketFile);
		socketOut.println(""+port);
		socketOut.close();
		RapidMiner.addShutdownHook(new Runnable() {
			@Override
			public void run() {
				LOGGER.config("Deleting "+socketFile);
				socketFile.delete();				
			}
		});
		this.handler = handler;
		while (true) {
			Socket client = serverSocket.accept();
			// We don't spawn another thread here. 
			// Assume no malicious client and communication is quick.
			talkToSecondClient(client);
		}
	}

	private void talkToSecondClient(Socket client) {
		try {
			LOGGER.info("Second client launched.");
			PrintStream out = new PrintStream(client.getOutputStream());
			out.println(HELLO_MESSAGE);
			Document doc = XMLTools.parse(client.getInputStream());
			LOGGER.config("Read XML document from other client: ");
			final String command = doc.getDocumentElement().getTagName();
			if ("args".equals(command)) {
				NodeList argsElems = doc.getDocumentElement().getElementsByTagName("arg");
				List<String> args = new LinkedList<String>();
				for (int i = 0; i < argsElems.getLength(); i++) {
					args.add(argsElems.item(0).getTextContent());
				}
				if (handler != null) {
					LOGGER.config("Handling <args> command from other client.");
					try {
						if (handler.handleArguments(args.toArray(new String[args.size()]))) {
							out.println(OK);
						} else {
							out.println(REJECTED);
						}
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Error executing remote control command: "+e, e);
						out.println(FAILED);
					}
				} else {
					LOGGER.warning("Other client sent <args> command, but I don't have a handler installed.");
					out.println(FAILED);
				}
			} else {
				out.println(UNKNOWN_COMMAND);
				LOGGER.warning("Unknown command from second client: <"+command+">.");	
			}
//			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
//			String line;
//			while ((line = in.readLine()) != null) {
//				LOGGER.info("Other client says: "+line);
//				out.println("You said: "+line);
//			}
			client.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to talk to client: "+e, e);
		} catch (SAXException e) {
			LOGGER.log(Level.WARNING, "I don't understand what the other client is trying to say: "+e, e);
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

//	private boolean isOtherInstanceUp() {
//		final Socket other = getOtherInstance();
//		if (other != null) {
//			boolean isRM;
//			try {
//				BufferedReader in = new BufferedReader(new InputStreamReader(other.getInputStream()));
//				isRM = readHelloMessage(in);				
//				other.close();
//			} catch (IOException e) {
//				LOGGER.log(Level.WARNING, "Failed to other instance: "+e, e);
//				return false;
//			}
//			return isRM;
//		} else {
//			return false;
//		}
//	}

	private boolean readHelloMessage(BufferedReader in) throws IOException {
		boolean isRM;
		String line = in.readLine();
		if (HELLO_MESSAGE.equals(line)) {
			LOGGER.config("Found other RapidMiner instance.");
			isRM = true;
		} else {
			LOGGER.config("Read unknown string from other instance: "+line);
			isRM = false;					
		}
		return isRM;
	}

	private boolean sendToOtherInstanceIfUp(String ... args) {
		final Socket other = getOtherInstance();
		if (other == null) {
			return false;
		}
		try {			
			BufferedReader in = new BufferedReader(new InputStreamReader(other.getInputStream()));
			boolean isRM = readHelloMessage(in);
			if (!isRM) {
				return false;
			} else {
				LOGGER.config("Sending arguments to other RapidMiner instance: "+Arrays.toString(args));
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				Element root = doc.createElement("args");
				doc.appendChild(root);
				for (String arg : args) {
					Element argElem = doc.createElement("arg");
					argElem.setTextContent(arg);
					root.appendChild(argElem);
				}
				XMLTools.stream(doc, other.getOutputStream(), null);
				return true;
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to talk to other instance: "+e, e);
			return false;
		} catch (ParserConfigurationException e) {
			LOGGER.log(Level.WARNING, "Cannot create XML document: "+e, e);
			return false;
		} catch (XMLException e) {
			LOGGER.log(Level.WARNING, "Cannot create XML document: "+e, e);
			return false;
		} finally {
			try {
				other.close();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Failed to close socket: "+e, e);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		LogService.getRoot();
		ParameterService.init();
		if (!getInstance().sendToOtherInstanceIfUp("hallo", "du", "da")) {
			getInstance().installListener(new RemoteControlHandler() {				
				@Override
				public boolean handleArguments(String[] args) {
					System.out.println("Received args: "+Arrays.toString(args));
					return true;
				}
			});
		} else {
			LOGGER.config("Other client already up. Exiting.");
		}
	}
}
