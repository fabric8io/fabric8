/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.zookeeper.commands;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static io.fabric8.zookeeper.utils.RegexSupport.getPatterns;
import static io.fabric8.zookeeper.utils.RegexSupport.matches;
import static io.fabric8.zookeeper.utils.RegexSupport.merge;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getAllChildren;

@Command(name = "export", scope = "zk", description = "Export the contents of the fabric registry to the specified directory in the filesystem", detailedDescription = "classpath:export.txt")
public class ExportAction extends ZooKeeperCommandSupport {

    @Argument(description="Path of the directory to export to")
    String target = System.getProperty("runtime.home") + File.separator + "fabric" + File.separator + "export";

    @Option(name="-f", aliases={"--regex"}, description="Specifies a regular expression that matches the znode paths you want to include in the export. For multiple include expressions, specify this option multiple times. The regular expression syntax is defined by the java.util.regex package.", multiValued=true)
    String regex[];

    @Option(name="-rf", aliases={"--reverse-regex"}, description="Specifies a regular expression that matches the znode paths you want to exclude from the export. For multiple exclude expressions, specify this option multiple times. The regular expression syntax is defined by the java.util.regex package.", multiValued=true)
    String nregex[];

    @Option(name="-p", aliases={"--path"}, description="Top level znode to export")
    String topLevel = "/";

    @Option(name="-d", aliases={"--delete"}, description="Delete the existing contents of the target directory before exporting. CAUTION: Performs a recursive delete! ")
    boolean delete;

    @Option(name="-t", aliases={"--trim"}, description="Trims the first timestamp comment line in properties files starting with the '#' character")
    boolean trimHeader;

    @Option(name="--dry-run", description="Log the actions that would be performed during an export, but do not actually perform the export.")
    boolean dryRun = false;

    File ignore = new File(".fabricignore");
    File include = new File(".fabricinclude");

    private final CuratorFramework curator;

    ExportAction(CuratorFramework curator) {
        this.curator = curator;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    protected void doExecute(CuratorFramework curator) throws Exception {
        nregex = merge(ignore, nregex);
        regex = merge(include, regex);
        export(curator, topLevel);
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

    protected void export(CuratorFramework curator, String path) throws Exception {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        List<Pattern> include = getPatterns(regex);
        List<Pattern> exclude = getPatterns(nregex);

        List<String> paths = getAllChildren(curator, path);
        SortedSet<File> directories = new TreeSet<File>();
        Map<File, String> settings = new HashMap<File, String>();

        boolean founMatch = false;
        for(String p : paths) {
            if (!matches(include, p, true) || matches(exclude, p, false)) {
                continue;
            }
            founMatch = true;
            byte[] data = curator.getData().forPath(p);
            String name = p;
            // FABRIC-1072 - we can't create paths containing '*'
            // there are more characters which are legal in ZK paths but illegal in different filesystems...
            if (name.contains("/*")) {
                name = name.replaceAll(Pattern.quote("/*"), "/__asterisk__");
            }
            if (data != null && data.length > 0) {
                //Znodes that translate into folders and also have data need to change their name to avoid a collision.
                if (!p.contains(".") || p.endsWith("fabric-ensemble")) {
                    name += ".cfg";
                }
                String value = new String(data);
                if (trimHeader && value.startsWith("#")) {
                    // lets remove the first line
                    int idx = value.indexOf("\n");
                    if (idx > 0) {
                        value = value.substring(idx + 1);
                    }
                }
                settings.put(new File(target + File.separator + name), value);
            } else {
                directories.add(new File(target + File.separator + name));
            }
        }

        if (!founMatch) {
            System.out.println("No entry matched the criteria.");
            return;
        }

        if (delete) {
            if (!dryRun) {
                delete(new File(target));
            } else {
                System.out.printf("Deleting %s and everything under it\n", new File(target));
            }
        }

        for (File d : directories) {
            if (d.exists() && !d.isDirectory()) {
                throw new IllegalArgumentException("Directory " + d + " exists but is not a directory");
            }
            if (!d.exists()) {
                if (!dryRun) {
                    if (!d.mkdirs()) {
                        throw new RuntimeException("Failed to create directory " + d);
                    }
                } else {
                    System.out.printf("Creating directory path : %s\n", d);
                }
            }
        }
        for (File f : settings.keySet()) {
            if (f.exists() && !f.isFile()) {
                throw new IllegalArgumentException("File " + f + " exists but is not a file");
            }
            if (!f.getParentFile().exists()) {
                if (!dryRun) {
                    if (!f.getParentFile().mkdirs()) {
                        throw new RuntimeException("Failed to create directory " + f.getParentFile());
                    }
                } else {
                    System.out.printf("Creating directory path : %s\n", f);
                }
            }
            if (!f.exists()) {
                try {
                    if (!dryRun) {
                        if (!f.createNewFile()) {
                            throw new RuntimeException("Failed to create file " + f);
                        }
                    } else {
                        System.out.printf("Creating file : %s\n", f);
                    }
                } catch (IOException io) {
                    throw new RuntimeException("Failed to create file " + f + " : " + io);
                }
            }
            if (!dryRun) {
                FileWriter writer = new FileWriter(f, false);
                writer.write(settings.get(f));
                writer.close();
            } else {
                System.out.printf("Writing value \"%s\" to file : %s\n", settings.get(f), f);
            }
        }

        System.out.printf("Export to %s completed successfully\n", target);
    }

    @Override
    protected Object doExecute() throws Exception {
        doExecute(getCurator());
        return null;
    }

}
