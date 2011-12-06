/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge.zk.internal;


import org.fusesource.fabric.zookeeper.commands.Import;

/**
 * User: dbokde
 * Date: 11/1/11
 * Time: 12:38 PM
 */
public class TestImport extends Import {

    public void setSource(String path) {
        super.source = path;
    }

    public void setNRegEx(String[] nRegEx) {
        super.nregex = nRegEx;
    }

    @Override
    public Object doExecute() throws Exception {
        return super.doExecute();
    }
}
