/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.hadoop.commands;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.NameNode;

@Command(name = "namenode-format", scope = "hadoop", description = "Format a HDFS volume")
public class NameNodeFormat extends HadoopCommandSupport {

    @Option(name = "--force")
    boolean force;

    @Override
    protected void doExecute(Configuration conf) throws Exception {
        Collection<File> dirsToFormat = FSNamesystem.getNamespaceDirs(conf);
        for (Iterator<File> it = dirsToFormat.iterator(); it.hasNext(); ) {
            File curDir = it.next();
            if (!curDir.exists()) {
                continue;
            }
            if (!force) {
                System.err.print("Re-format filesystem in " + curDir + " ? (Y or N) ");
                System.err.flush();
                if (!(System.in.read() == 'Y')) {
                    System.err.println("Format aborted in " + curDir);
                    return;
                }
                while (System.in.read() != '\n') ; // discard the enter-key
            }
        }
        NameNode.format(conf);
    }

}
