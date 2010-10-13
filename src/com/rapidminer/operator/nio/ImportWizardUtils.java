package com.rapidminer.operator.nio;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.SwingTools;

/**
 * 
 * @author Simon Fischer
 *
 */
public class ImportWizardUtils {

	public static void showErrorMessage(String resource, String message, Throwable exception) {
		SwingTools.showSimpleErrorMessage("importwizard.io_error", exception, resource, message);
	}

	public static int getPreviewLength() {
		try {
			return Integer.parseInt(RapidMiner.getRapidMinerPropertyValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_MAX_TEST_ROWS));
		} catch (NumberFormatException e) {
			return 100;
		}
	}
	
}
