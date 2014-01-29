/**
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.commands;

import java.io.File;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.zookeeper.utils.ZookeeperImportUtils;

import static io.fabric8.zookeeper.utils.RegexSupport.merge;

@Command(name = "import", scope = "fabric", description = "Import data either from a filesystem or from a properties file into the fabric registry (ZooKeeper tree)", detailedDescription = "classpath:import.txt")
public class Import extends FabricCommand {

    @Argument(description = "Location of a filesystem (if --filesystem is specified) or a properties file (if --properties is specified).")
    protected String source = System.getProperty("karaf.home") + File.separator + "fabric" + File.separator + "import";

    @Option(name="-d", aliases={"--delete"}, description="Delete any paths not in the tree being imported. Ignored when importing a properties file. CAUTION: Using this option could permanently delete all or part of the fabric registry.")
    boolean delete = false;

    @Option(name="-t", aliases={"--target"}, description="Path of the znode that the data is imported into.")
    String target = "/";

    @Option(name="-props", aliases={"--properties"}, description="Indicates that the 'source' argument is a properties file.")
    boolean properties = false;

    @Option(name="-fs", aliases={"--filesystem"}, description="Indicates that the 'source' argument is a directory on the filesystem.")
    boolean filesystem = true;

    @Option(name="-v", aliases={"--verbose"}, description="Verbose output of files being imported")
    boolean verbose = false;

    @Option(name="-f", aliases={"--regex"}, description="Specifies a regular expression that matches the znode paths you want to include in the import. For multiple include expressions, specify this option multiple times. The regular expression syntax is defined by the java.util.regex package.", multiValued=true)
    String regex[];

    @Option(name="-rf", aliases={"--reverse-regex"}, description="Specifies a regular expression that matches the znode paths you want to exclude from the import. For multiple exclude expressions, specify this option multiple times. The regular expression syntax is defined by the java.util.regex package.", multiValued=true)
    protected String[] nregex;

    @Option(name="-p", aliases={"--profile"}, multiValued = true, description="Import the specified profile")
    String[] profiles;

    @Option(name="--version", multiValued = true, description="Import the specified version")
    String[] versions;

    @Option(name="--dry-run", description="Log the actions that would be performed during an import, but do not actually perform the import.")
    boolean dryRun = false;

    File ignore = new File(".fabricignore");
    File include = new File(".fabricinclude");

    protected void doExecute(CuratorFramework zk) throws Exception {

        nregex = merge(ignore, nregex, null, null);
        regex = merge(include, regex, versions, profiles);

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

    @Override
    protected Object doExecute() throws Exception {
        doExecute(getCurator());
        return null;
    }
}

