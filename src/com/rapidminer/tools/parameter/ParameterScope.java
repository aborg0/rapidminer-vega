package com.rapidminer.tools.parameter;

/**
 * The ParameterScope defines where the associated Parameter is used.
 * PreStartParameters will be exported with the key of their parameter type as an environment variable.
 * This can be used for example for setting up things that need to be influenced before the jvm starts.
 * 
 * ModifyingPreStartParameters will be appended to existing environment variables.
 * 
 * @author Sebastian Land
 */
public class ParameterScope {
    private String preStartName = null;
    private boolean isModifyingPreStartParameter = false;
    private boolean isGuiParameter = false;
    private boolean isFileAccessParameter = false;

    /**
     * This is this a preStartParameter, this will return the
     * name of the environment variable that should be set by this parameter. Otherwise null
     * is returned.
     */
    public String getPreStartName() {
        return preStartName;
    }

    public boolean isPreStartParameter() {
        return preStartName == null;
    }

    public boolean isGuiParameter() {
        return isGuiParameter;
    }

    public boolean isFileAccessParameter() {
        return isFileAccessParameter;
    }

    public boolean isModifyingPreStartParameter() {
        return isModifyingPreStartParameter;
    }
}