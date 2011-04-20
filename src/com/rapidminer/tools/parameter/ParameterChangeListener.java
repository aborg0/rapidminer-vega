package com.rapidminer.tools.parameter;

import com.rapidminer.tools.ParameterService;

/**
 * This interface can be implemented in order to be informed whenever a parameter
 * in the {@link ParameterService} changes.
 * It replaces the ancient SettingsChangeListener that only was informed,
 * when the user changed a parameter.
 *
 * @author Sebastian Land
 */
public interface ParameterChangeListener {
    /**
     * This method will be invoked whenever a parameter of the ParameterService
     * has been changed.
     */
    public void informParameterChanged(String key, String value);

    /**
     * This method will be called whenever the settings will be saved.
     */
    public void informParameterSaved();
}