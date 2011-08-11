/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

@Command(name = "export", scope = "zk", description = "Export data from zookeeper")
public class Export extends ZooKeeperCommandSupport {

    @Argument(description="URL of the file to export to")
    String target = "." + File.separator + "export";

    @Option(name="-f", aliases={"--regex"}, description="regex to filter on what paths to export", multiValued=true)
    String regex[];

    @Option(name="-p", aliases={"--path"}, description="Top level context to export")
    String topLevel = "/";

    @Option(name="-d", aliases={"--delete"}, description="Clear target directory before exporting (CAUTION! Performs recursive delete!)")
    boolean delete;

    @Override
    protected Object doExecute() throws Exception {
        export(topLevel);
        System.out.printf("Export to %s completed successfully\n", target);
        return null;
    }

    private boolean matches(List<Pattern> patterns, String value) {
        if (patterns.isEmpty()) {
            return true;
        }
        boolean rc = false;
        for (Pattern pattern : patterns) {
            if (pattern.matcher(value).matches()) {
                rc = true;
            }
        }
        return rc;
    }

    private void delete(File parent) throws Exception {
        if (!parent.exists()) {
            return;
        }
        if (parent.isDirectory()) {
            for (File f : parent.listFiles()) {
                delete(f);
            }
        }
        parent.delete();
    }

    protected void export(String path) throws Exception {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        List<Pattern> patterns = new ArrayList<Pattern>();
        if (regex != null) {
            for(String p : regex) {
                patterns.add(Pattern.compile(p));
            }
        }
        List<String> paths = getZooKeeper().getAllChildren(path);
        SortedSet<File> directories = new TreeSet<File>();
        Map<File, String> settings = new HashMap<File, String>();

        for(String p : paths) {
            p = path + p;
            if (!matches(patterns, p)) {
                continue;
            }
            byte[] data = getZooKeeper().getData(p);
            if (data != null) {
                settings.put(new File(target + File.separator + p + ".cfg"), new String(data));
            } else {
                directories.add(new File(target + File.separator + p));
            }
        }

        if (delete) {
            delete(new File(target));
        }

        for (File d : directories) {
            if (d.exists() && !d.isDirectory()) {
                throw new IllegalArgumentException("Directory " + d + " exists but is not a directory");
            }
            if (!d.exists()) {
                if (!d.mkdirs()) {
                    throw new RuntimeException("Failed to create directory " + d);
                }
            }
        }
        for (File f : settings.keySet()) {
            if (f.exists() && !f.isFile()) {
                throw new IllegalArgumentException("File " + f + " exists but is not a file");
            }
            if (!f.getParentFile().exists()) {
                if (!f.getParentFile().mkdirs()) {
                    throw new RuntimeException("Failed to create directory " + f.getParentFile());
                }
            }
            if (!f.exists()) {
                try {
                    if (!f.createNewFile()) {
                        throw new RuntimeException("Failed to create file " + f);
                    }
                } catch (IOException io) {
                    throw new RuntimeException("Failed to create file " + f + " : " + io);
                }
            }
            FileWriter writer = new FileWriter(f, false);
            writer.write(settings.get(f));
            writer.close();
        }
    }
}
