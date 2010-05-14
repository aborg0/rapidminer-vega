package com.rapidminer.tools;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import com.rapidminer.RapidMiner;

/** Makes a session based on RapidMiner properties.
 * 
 * @author Simon Fischer
 *
 */
public class DefaultMailSessionFactory implements MailSessionFactory {

	@Override
	public Session makeSession() {
		String host = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST);
		if (host == null) {
			LogService.getRoot().warning("Must specify SMTP host in "+RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST+" to use SMTP.");
			return null;
		} else {
			Properties props = new Properties();
			props.put("mail.smtp.host", host);
			props.put("mail.from", "no-reply@rapidminer.com");
			final String user = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_USER);
			props.put("mail.user", user);
			final String passwd = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_PASSWD);
			Authenticator authenticator = null;				
			if ((passwd != null) && (passwd.length() > 0)) {
				props.setProperty("mail.smtp.submitter", user);
				props.setProperty("mail.smtp.auth", "true");
				authenticator = new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(user, passwd);
					}
				};
			}
			String port = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_PORT);
			if (port != null) {
				props.setProperty("mail.smtp.port", port);
			}
			return Session.getInstance(props, authenticator);
		}
	}

}
