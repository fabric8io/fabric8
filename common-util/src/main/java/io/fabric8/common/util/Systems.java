package io.fabric8.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Systems {
    private static final transient Logger LOG = LoggerFactory.getLogger(Systems.class);

    /**
     * Returns the value of the given environment variable if its not blank or the given default value
     */
    public static String getEnvVar(String envVarName, String defaultValue) {
        String envVar = null;
        try {
            envVar = System.getenv(envVarName);
        } catch (Exception e) {
            LOG.warn("Failed to look up environment variable $" + envVarName + ". " + e, e);
        }
        if (Strings.isNotBlank(envVar)) {
            return envVar;
        } else {
            return defaultValue;
        }
    }
}
