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

import java.io.File;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.zookeeper.utils.ZookeeperImportUtils;
import org.linkedin.zookeeper.client.IZKClient;

import static org.fusesource.fabric.zookeeper.utils.RegexSupport.merge;

@Command(name = "import", scope = "zk", description = "Import data into zookeeper", detailedDescription = "classpath:import.txt")
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
    protected void doExecute(IZKClient zk) throws Exception {
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
        if (properties) {
            ZookeeperImportUtils.importFromPropertiesFile(zk, source, target, regex, nregex, dryRun);
        }
        if (filesystem) {
            ZookeeperImportUtils.importFromFileSystem(zk, source, target, regex, nregex, delete, dryRun, verbose);
        }
        System.out.println("imported ZK data from: " + source);
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

