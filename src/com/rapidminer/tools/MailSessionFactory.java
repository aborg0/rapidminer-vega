package com.rapidminer.tools;

import javax.mail.Session;

/** Creates mail sessions.
 * 
 * @author Simon Fischer
 *
 */
public interface MailSessionFactory {

	public Session makeSession();
}
