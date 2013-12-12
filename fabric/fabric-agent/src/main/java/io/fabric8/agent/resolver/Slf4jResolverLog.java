package io.fabric8.agent.resolver;

import org.slf4j.Logger;

/**
 */
public class Slf4jResolverLog extends org.apache.felix.resolver.Logger {

    private final Logger logger;

    public Slf4jResolverLog(Logger logger) {
        super(LOG_DEBUG);
        this.logger = logger;
    }

    @Override
    protected void doLog(int level, String msg, Throwable throwable) {
        switch (level) {
            case LOG_ERROR:
                logger.error(msg, throwable);
                break;
            case LOG_WARNING:
                logger.warn(msg, throwable);
                break;
            case LOG_INFO:
                logger.info(msg, throwable);
                break;
            case LOG_DEBUG:
                logger.debug(msg, throwable);
                break;
        }
    }
}
