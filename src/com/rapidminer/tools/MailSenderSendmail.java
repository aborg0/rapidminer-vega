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

import java.io.PrintStream;

import com.rapidminer.RapidMiner;

/**
 * Sends a mail via sendmail.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class MailSenderSendmail implements MailSender {

	public void sendEmail(String address, String subject, String content) throws Exception {
		String command = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SENDMAIL_COMMAND);
		if ((command == null) || (command.length() > 0)) {
			LogService.getGlobal().log("Must specify sendmail command to use sendmail.", LogService.ERROR);
		} else {					
			LogService.getGlobal().log("Executing '" + command + "'", LogService.MINIMUM);
			Process sendmail = Runtime.getRuntime().exec(new String[] { command, address });
			PrintStream out = null;
			try {
				out = new PrintStream(sendmail.getOutputStream());
				out.println("Subject: " + subject);
				out.println("From: RapidMiner");
				out.println("To: " + address);
				out.println();
				out.println(content);
			} catch (Exception e) {
				throw e;
			} finally {
				if (out != null)
					out.close();
			}
			Tools.waitForProcess(null, sendmail, command);
		}
	}
}
