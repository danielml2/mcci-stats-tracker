package me.danielml.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToggleableLogger {

    private Logger logger;
    private boolean enabled = true;

    public ToggleableLogger(String name) {
        logger = LoggerFactory.getLogger(name);
    }

    public void info(String message) {
        if(enabled)
            logger.info(message);
    }

    public void debug(String message) {
        if(enabled)
            logger.debug(message);
    }

    public void warn(String message) {
        if(enabled)
            logger.warn(message);
    }

    public void error(String message) {
        if(enabled)
            logger.error(message);
    }
    public void error(String message, Exception exception) {
        if(enabled)
            logger.error(message, exception);
    }

    public void forceWarn(String message) {
        logger.warn(message);
    }

    public void forceError(String message, Exception e) {
        logger.error(message, e);
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
