/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

import java.util.Map;

public interface Profile {

    String getId();
    String getVersion();

    Profile[] getParents();
    void setParents(Profile[] parents);

    Map<String, Map<String, String>> getConfigurations();

}
