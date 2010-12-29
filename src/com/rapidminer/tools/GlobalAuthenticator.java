/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.PasswordDialog;

/** Global authenticator at which multiple other authenticators can register.
 *  Authentication requests will be delegated subsequently until an authenticator
 *  is found.
 * 
 * @author Simon Fischer
 *
 */
public class GlobalAuthenticator extends Authenticator {

	private final List<URLAuthenticator> registeredAuthenticators = new LinkedList<URLAuthenticator>();
	
	private static final GlobalAuthenticator THE_INSTANCE = new GlobalAuthenticator();
	
	public interface URLAuthenticator {
		public PasswordAuthentication getAuthentication(URL url);
		public String getName();
	}
	
    private static class ProxyAuthenticator extends Authenticator {
        private String username;
        private String password;
        public ProxyAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }
        protected PasswordAuthentication getPasswordAuthentication() {
            return(new PasswordAuthentication(this.username, this.password.toCharArray()));
        }
    }

	
	static {
		Authenticator.setDefault(THE_INSTANCE);
	}
	
	public synchronized static void register(URLAuthenticator authenticator) {
		THE_INSTANCE.registeredAuthenticators.add(authenticator);
	}
	
	@Override
	protected synchronized PasswordAuthentication getPasswordAuthentication() {
		URL url = getRequestingURL();
		LogService.getRoot().info("Authentication requested for: "+url+". Trying these authenticators: "+registeredAuthenticators+".");
		for (URLAuthenticator a : registeredAuthenticators) {
			PasswordAuthentication auth =  a.getAuthentication(url);
			if (auth != null) {
				return auth;
			}
		}
		
		//LogService.getRoot().info("Authentication requested for unknown URL: "+url);
		//return PasswordDialog.getPasswordAuthentication(url.toString(), false, false);
		
		if ("http".equals(url.getProtocol())) {
			if (Tools.booleanValue(System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_SET), false)) {
				String username = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_USERNAME);
				String password = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_PASSWORD);
				if ((username != null) && (username.length() > 0) && (password != null) && (password.length() > 0)) {
					LogService.getRoot().info("Authentication requested for unknown URL, trying proxy authentication: "+url);
					return new ProxyAuthenticator(username, password).getPasswordAuthentication();
				} else {
					LogService.getRoot().info("Authentication requested for unknown URL: "+url);
					return PasswordDialog.getPasswordAuthentication(url.toString(), false, false);
				}
			} else {
				LogService.getRoot().info("Authentication requested for unknown URL: "+url);
				return PasswordDialog.getPasswordAuthentication(url.toString(), false, false);
			}
		} else {
			LogService.getRoot().info("Authentication requested for unknown URL: "+url);
			return PasswordDialog.getPasswordAuthentication(url.toString(), false, false);
		}
	}

	public static void init() {}	
}
