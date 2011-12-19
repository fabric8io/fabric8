/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.commands.support.EnsembleCommandSupport;
import org.fusesource.fabric.zookeeper.commands.Import;

@Command(name = "ensemble-create", scope = "fabric", description = "Create a new ZooKeeper ensemble", detailedDescription = "classpath:ensemble.txt")
public class EnsembleCreate extends EnsembleCommandSupport {

    @Option(name = "--clean", description = "Clean local zookeeper cluster and configurations")
    private boolean clean;

    @Option(name = "--no-import", description = "Disable the import of the sample registry data from ")
    private boolean noImport;

    @Option(name = "--import-dir", description = "Directory of files to import into the newly created ensemble")
    private String importDir = getDefaultImportDir();

    @Argument(required = false, multiValued = true, description = "List of agents")
    private List<String> agents;

    @Override
    protected Object doExecute() throws Exception {
        if (clean) {
            service.clean();
        } else {
            if (agents == null || agents.isEmpty()) {
                throw new IllegalStateException("No agents specified.");
            }
        }
        if (agents != null && !agents.isEmpty()) {
            service.createCluster(agents);

            // now lets populate the registry with files from a mvn plugin
            if (!noImport) {
                Import tool = new Import();
                tool.setBundleContext(getBundleContext());
                tool.setZooKeeper(service.getZooKeeper());
                tool.setSource(importDir);
                return tool.execute(session);
            }
        }
        return null;
    }

    private static String getDefaultImportDir() {
        return System.getProperty("karaf.home", ".") + "/import";
    }

}
