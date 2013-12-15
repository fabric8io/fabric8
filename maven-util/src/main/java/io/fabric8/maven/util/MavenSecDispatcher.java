package io.fabric8.maven.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

public class MavenSecDispatcher extends DefaultSecDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenSecDispatcher.class);

    public MavenSecDispatcher() {
        _configurationFile = "~/.m2/settings-security.xml";
        try {
            _cipher = new DefaultPlexusCipher();
        } catch (PlexusCipherException e) {
            LOGGER.error("Error creating sec dispatcher");
        }
    }
}
