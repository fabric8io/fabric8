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

package org.fusesource.fabric.internal;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.ZooKeeperClusterService;

public class ContainerProviderUtils {

    private static final String REPLACE_FORMAT = "sed -i  \"s/%s/%s/g\" %s";
    private static final String LINE_APPEND = "sed  's/%s/&%s/' %s > %s";

    public static final int DEFAULT_SSH_PORT = 8101;

    private ContainerProviderUtils() {
        //Utility Class
    }

    public static String buildStartupScript(CreateContainerOptions options) throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        sb.append("function run { echo \"Running: $*\" ; $* ; rc=$? ; if [ \"${rc}\" -ne 0 ]; then echo \"Command failed\" ; exit ${rc} ; fi ; }\n");
        sb.append("run mkdir -p ~/containers/ ").append("\n");
        sb.append("run cd ~/containers/ ").append("\n");
        sb.append("run mkdir -p ").append(options.getName()).append("\n");
        sb.append("run cd ").append(options.getName()).append("\n");
        extractTargzIntoDirectory(sb, options.getProxyUri(), "org.fusesource.fabric", "fuse-fabric", FabricConstants.FABRIC_VERSION);
        sb.append("run cd ").append("fuse-fabric-" + FabricConstants.FABRIC_VERSION).append("\n");
        List<String> lines = new ArrayList<String>();
        appendFile(sb, "etc/startup.properties", lines);
        replaceLineInFile(sb, "etc/system.properties", "karaf.name=root", "karaf.name = " +options.getName());
        replaceLineInFile(sb,"etc/org.apache.karaf.shell.cfg","sshPort=8101","sshPort="+DEFAULT_SSH_PORT);
        appendFile(sb, "etc/system.properties",Arrays.asList("\n"));
        if(options.isEnsembleServer()) {
            appendFile(sb, "etc/system.properties", Arrays.asList(ZooKeeperClusterService.ENSEMBLE_AUTOSTART +"=true"));
        } else {
            appendFile(sb, "etc/system.properties", Arrays.asList("zookeeper.url = " + options.getZookeeperUrl()));
        }
        if(options.isDebugContainer()) {
           sb.append("run export KARAF_DEBUG=true").append("\n");
        }
        appendToLineInFile(sb,"etc/org.apache.karaf.features.cfg","featuresBoot=","fabric-agent,");
        sb.append("run nohup bin/start").append("\n");
        return sb.toString();
    }

    private static String downloadAndStartMavenBundle(StringBuilder sb, URI proxy, String groupId, String artifactId, String version, String type) {
        String path = groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version;
        String file = path + "/" + artifactId + "-" + version + "." + type;
        sb.append("run mkdir -p " + "system/").append(path).append("\n");
        sb.append("run curl --show-error --silent --get --retry 20 --output system/").append(file).append(" ").append(proxy.resolve(file)).append("\n");
        return file;
    }

    private static void replaceLineInFile(StringBuilder sb, String path, String pattern, String line) {
        sb.append(String.format(REPLACE_FORMAT,pattern,line,path)).append("\n");
    }

    private static void appendToLineInFile(StringBuilder sb, String path, String pattern, String line) {
        sb.append(String.format(LINE_APPEND,pattern,line,path,path+".tmp")).append("\n");
        sb.append("mv "+path+".tmp "+path).append("\n");
    }

    private static void appendFile(StringBuilder sb, String path, Iterable<String> lines) {
        final String MARKER = "END_OF_FILE";
        sb.append("cat >> ").append(path).append(" <<'").append(MARKER).append("'\n");
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        sb.append(MARKER).append("\n");
    }

    private static void extractTargzIntoDirectory(StringBuilder sb, URI proxy, String groupId, String artifactId, String version) {

        String file = artifactId + "-" + version + ".tar.gz";
        String directory =  groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version + "/";
        String artifactParentUri = proxy.resolve(directory).toString();
        String artifactUri = proxy.resolve(directory+file).toString();

        //To cover the case of SNAPSHOT dependencies where URL can't be determined we are quering for the availale versions first
        if(version.contains("SNAPSHOT")) {
            sb.append("run export DISTRO_URL=`curl --silent ").append(artifactParentUri).append("| grep href | grep \"tar.gz\\\"\" | sed 's/^.*<a href=\"//' | sed 's/\".*$//'  | tail -1`").append("\n");
        } else {
            sb.append("run export DISTRO_URL=`").append(artifactUri).append("`").append("\n");
        }
        sb.append("if [[  \"$DISTRO_URL\" == \"\" ]] ;  then export DISTRO_URL=").append(artifactUri).append("; fi\n");
        sb.append("run curl --show-error --silent --get --retry 20 --output ").append(file).append(" ").append("$DISTRO_URL").append("\n");
        sb.append("run tar -xpzf ").append(file).append("\n");
    }
}
