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
package com.rapidminer.operator.ports.metadata;

import java.util.Collections;
import java.util.List;

import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.quickfix.QuickFix;

/** 
 * @author Simon Fischer
 *
 */
public class SimpleMetaDataError extends SimpleProcessSetupError implements MetaDataError {

	private Port port;

	public SimpleMetaDataError(Severity severity, Port port, String i18nKey, Object ... i18nArgs) {
		this(severity, port, Collections.<QuickFix>emptyList(), i18nKey, i18nArgs);
	}

	public SimpleMetaDataError(Severity severity, Port port, List<? extends QuickFix> fixes, String i18nKey, Object ... args) {
		super(severity, port == null ? null : port.getPorts().getOwner(), fixes, true, "metadata.error." + i18nKey, args);
		this.port = port;
	}


	@Override
	public Port getPort() {
		return port;
	}
}



