/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.zookeeper.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKClient;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import static org.fusesource.fabric.zookeeper.commands.RegexSupport.getPatterns;
import static org.fusesource.fabric.zookeeper.commands.RegexSupport.matches;
import static org.fusesource.fabric.zookeeper.commands.RegexSupport.merge;

@Command(name = "import", scope = "zk", description = "Import data into zookeeper")
public class Import extends ZooKeeperCommandSupport {

    @Argument(description = "Location of the file or filesystem to load")
    protected String source = "." + File.separator + "import";

    @Option(name="-d", aliases={"--delete"}, description="Delete any paths not in the tree being imported, ignored when importing a properties file (CAUTION!)")
    boolean delete = false;

    @Option(name="-t", aliases={"--target"}, description="Target location in ZooKeeper tree to import to")
    String target = "/";

    @Option(name="-props", aliases={"--properties"}, description="Argument is URL pointing to a properties file")
    boolean properties = false;

    @Option(name="-fs", aliases={"--filesystem"}, description="Argument is the top level directory of a local filesystem tree")
    boolean filesystem = true;

    @Option(name="-v", aliases={"--verbose"}, description="Verbose output of files being imported")
    boolean verbose = false;

    @Option(name="-f", aliases={"--regex"}, description="regex to filter on what paths to import, can specify this option more than once for additional filters", multiValued=true)
    String regex[];

    @Option(name="-rf", aliases={"--reverse-regex"}, description="regex to filter what paths to exclude, can specify this option more than once for additional filters", multiValued=true)
    protected String[] nregex;

    @Option(name="--dry-run", description="Runs the import but prints out what's going to happen instead of making any changes")
    boolean dryRun = false;

    File ignore = new File(".fabricignore");
    File include = new File(".fabricinclude");

    @Override
    protected Object doExecute() throws Exception {
        if (ignore.exists() && ignore.isFile()) {
            nregex = merge(ignore, nregex);
        }
        if (include.exists() && include.isFile()) {
            regex = merge(include, regex);
        }
        if (properties == true) {
            filesystem = false;
        }
        if (filesystem == true) {
            properties = false;
        }
        checkZooKeeperConnected();
        if (properties) {
            readPropertiesFile();
        }
        if (filesystem) {
            readFileSystem();
        }
        System.out.println("imported ZK data from: " + source);
        return null;
    }

    private String buildZKPath(File parent, File current) {
        String rc = "";
        if (current != null && !parent.equals(current)) {
            rc = buildZKPath(parent, current.getParentFile()) + "/" + current.getName();
        }
        return rc;
    }

    private void getCandidates(File parent, File current, Map<String, String> settings) throws Exception {
        List<Pattern> profile = getPatterns(new String[]{RegexSupport.PROFILE_REGEX});
        List<Pattern> agentProperties = getPatterns(new String[]{RegexSupport.PROFILE_AGENT_PROPERTIES_REGEX});
        if (current.isDirectory()) {
            for (File child : current.listFiles()) {
                getCandidates(parent, child, settings);
            }
            String p = buildZKPath(parent, current).replaceFirst("/", "");
            settings.put(p, null);
        } else {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(current));
            byte[] contents = new byte[in.available()];
            in.read(contents);
            in.close();
            String p = buildZKPath(parent, current).replaceFirst("/", "");
            if (p.endsWith(".cfg")) {
                p = p.substring(0, p.length() - ".cfg".length());
            }

            if (matches(agentProperties,p,false)) {
                settings.put(p, new String(contents).replaceAll(RegexSupport.PARENTS_REGEX,""));
                String parents = Pattern.compile(RegexSupport.PARENTS_REGEX).matcher(p).toMatchResult().group();
                settings.put(p.substring(0,p.lastIndexOf("/")),parents);
            } else if (!matches(profile,p,false)) {
                settings.put(p, new String(contents));
            }
        }
    }

    private void readFileSystem() throws Exception {
        Map<String, String> settings = new TreeMap<String, String>();
        File s = new File(source);
        getCandidates(s, s, settings);
        List<Pattern> include = getPatterns(regex);
        List<Pattern> exclude = getPatterns(nregex);

        if (!target.endsWith("/")) {
            target = target + "/";
        }
        if (!target.startsWith("/")) {
            target = "/" + target;
        }

        List<String> paths = new ArrayList<String>();

        for(String key : settings.keySet()) {
            String data = settings.get(key);
            key = target + key;
            paths.add(key);
            if (!matches(include, key, true) || matches(exclude, key, false)) {
                continue;
            }
            if (!dryRun) {
                if (data != null) {
                    if (verbose) {
                        System.out.println("importing: " + key);
                    }
                    getZooKeeper().createOrSetWithParents(key, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            } else {
                System.out.printf("Creating path \"%s\" with value \"%s\"\n", key, data);
            }
        }

        if (delete) {
            deletePathsNotIn(paths);
        }
    }

    private void deletePathsNotIn(List<String> paths) throws Exception {
        List<String> zkPaths = getZooKeeper().getAllChildren(target);

        for (String path : zkPaths) {
            path = "/" + path;
            if (!paths.contains(path)) {
                if (!dryRun) {
                    getZooKeeper().deleteWithChildren(path);
                } else {
                    System.out.printf("Deleting path %s and everything under it\n", path);
                }
            }
        }

    }

    private void readPropertiesFile() throws Exception {
        List<Pattern> includes = getPatterns(regex);
        List<Pattern> excludes = getPatterns(nregex);
        InputStream in = new BufferedInputStream(new URL(source).openStream());
        List<String> paths = new ArrayList<String>();
        Properties props = new Properties();
        props.load(in);
        for (Enumeration names = props.propertyNames(); names.hasMoreElements();) {
            String name = (String) names.nextElement();
            String value = props.getProperty(name);
            if (value != null && value.isEmpty()) {
                value = null;
            }
            if (!name.startsWith("/")) {
                name = "/" + name;
            }
            name = target + name;
            if (!matches(includes, name, true) || matches(excludes, name, false)) {
                continue;
            }
            if (!dryRun) {
                getZooKeeper().createOrSetWithParents(name, value, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } else {
                System.out.printf("Creating path \"%s\" with value \"%s\"\n", name, value);
            }
        }
    }


    public boolean isFilesystem() {
        return filesystem;
    }

    public void setFilesystem(boolean filesystem) {
        this.filesystem = filesystem;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
