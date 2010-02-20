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

import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import com.rapidminer.RapidMiner;

/**
 * Sends a mail via SMTP.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class MailSenderSMTP implements MailSender {

	public void sendEmail(String address, String subject, String content) throws Exception {
		String host = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST);
		if (host == null) {
			LogService.getGlobal().log("Must specify SMTP host to use SMTP.", LogService.ERROR);
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
			Session session = Session.getInstance(props, authenticator);

			MimeMessage msg = new MimeMessage(session);				
			msg.setRecipients(Message.RecipientType.TO, address);
			msg.setFrom();
			msg.setSubject(subject);
			msg.setSentDate(new Date());
			msg.setText(content);					
			Transport.send(msg);
		}
	}
}
