/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.patch;

import java.net.URL;

public interface Service {

    Iterable<Patch> getPatches();
    
    Patch getPatch(String id);
    
    Iterable<Patch> download(URL url);

}
