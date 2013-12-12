/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 */
package io.fabric8.stream.log;

import java.util.HashMap;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
abstract public class Processor {

    public void start() throws Exception {}
    public void stop() {}

    public abstract void send(HashMap<String, String> headers, byte[] data, Callback onComplete);

}
