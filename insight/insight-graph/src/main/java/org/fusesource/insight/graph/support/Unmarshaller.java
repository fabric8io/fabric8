/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.insight.graph.support;

import java.io.IOException;

/**
 */
public interface Unmarshaller<T> {
    public T unmarshal(String path, byte[] data) throws IOException;
}
