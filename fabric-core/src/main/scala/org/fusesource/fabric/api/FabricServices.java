/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Helper methods for working with {@link FabricService} objects
 */
public class FabricServices {
    public static final String DEFAULT_MAVEN_REPO = "http://repo.fusesource.com/nexus/content/groups/public";

    public static URI getMavenRepoURI(FabricService fabricService, URI localMavenProxyURI) throws URISyntaxException {
        // lets default to the URI from ZK
        URI answer = fabricService.getMavenRepoURI();
        if (answer == null) {
            answer = localMavenProxyURI;
        }
        if (answer == null) {
            answer = new URI(DEFAULT_MAVEN_REPO);
        }
        return answer;

    }
}
