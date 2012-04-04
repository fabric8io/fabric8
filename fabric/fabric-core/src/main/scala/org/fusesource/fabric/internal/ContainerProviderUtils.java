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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.fusesource.fabric.utils.HostUtils;
import org.fusesource.fabric.zookeeper.ZkDefs;

public class ContainerProviderUtils {

    public static final String ADDRESSES_PROPERTY_KEY = "addresses";
    private static final String REPLACE_FORMAT = "sed -i  \"s/%s/%s/g\" %s";
    private static final String LINE_APPEND = "sed  's/%s/&%s/' %s > %s";
    private static final String FIRST_FABRIC_DIRECTORY = "ls -l | grep fuse-fabric | grep ^d | awk '{ print $NF }' | sort -n | head -1";

    private static final String RUN_FUCNTION = loadFunction("run.sh");
    private static final String DOWNLOAD_FUCNTION = loadFunction("download.sh");

    public static final int DEFAULT_SSH_PORT = 8101;

    private static final String[] FALLBACK_REPOS = {"http://repo.fusesource.com/nexus/content/groups/public/","http://repo.fusesource.com/nexus/content/groups/ea/"};

    private ContainerProviderUtils() {
        //Utility Class
    }

    /**
     * Creates a shell script for installing and starting up a container.
     * @param options
     * @return
     * @throws MalformedURLException
     */
    public static String buildInstallAndStartScript(CreateContainerOptions options) throws MalformedURLException, URISyntaxException {
        StringBuilder sb = new StringBuilder();
        sb.append(RUN_FUCNTION).append("\n");
        sb.append(DOWNLOAD_FUCNTION).append("\n");
        sb.append("run mkdir -p ~/containers/ ").append("\n");
        sb.append("run cd ~/containers/ ").append("\n");
        sb.append("run mkdir -p ").append(options.getName()).append("\n");
        sb.append("run cd ").append(options.getName()).append("\n");
        extractTargzIntoDirectory(sb, options.getProxyUri(), "org.fusesource.fabric", "fuse-fabric", FabricConstants.FABRIC_VERSION);
        sb.append("run cd `").append(FIRST_FABRIC_DIRECTORY).append("`\n");
        List<String> lines = new ArrayList<String>();
        lines.add(ZkDefs.GLOBAL_RESOLVER_PROPERTY + "=" + options.getResolver());
        appendFile(sb, "etc/system.properties", lines);
        replaceLineInFile(sb, "etc/system.properties", "karaf.name=root", "karaf.name = " + options.getName());
        replaceLineInFile(sb, "etc/org.apache.karaf.shell.cfg", "sshPort=8101", "sshPort=" + DEFAULT_SSH_PORT);
        appendFile(sb, "etc/system.properties", Arrays.asList("\n"));

        //Read all system properties
        for (Map.Entry<String, Properties> entry : options.getSystemProperties().entrySet()) {
            Properties addresses = entry.getValue();
            for (Object type : addresses.keySet()) {
                Object value = addresses.get(type);
                appendFile(sb, "etc/system.properties", Arrays.asList(type + "=" + value));
            }
        }

        //TODO: Be simple & move all of the code below under system properties MAP.
        if (options.getPreferredAddress() != null) {
            appendFile(sb, "etc/system.properties", Arrays.asList(HostUtils.PREFERED_ADDRESS_PROPERTY_NAME + "=" + options.getPreferredAddress()));
        }

        if(options.isEnsembleServer()) {
            appendFile(sb, "etc/system.properties", Arrays.asList(ZooKeeperClusterService.ENSEMBLE_AUTOSTART +"=true"));
        }

        if (options.getZookeeperUrl() != null) {
            appendFile(sb, "etc/system.properties", Arrays.asList("zookeeper.url = " + options.getZookeeperUrl()));
        }
        if(options.getJvmOpts() != null && !options.getJvmOpts().isEmpty()) {
           sb.append("run export JAVA_OPTS=").append(options.getJvmOpts()).append("\n");
        }

        appendToLineInFile(sb,"etc/org.apache.karaf.features.cfg","featuresBoot=","fabric-agent,");
        //Add the proxyURI to the list of repositories
        appendToLineInFile(sb,"etc/org.ops4j.pax.url.mvn.cfg","repositories=",options.getProxyUri().toString()+",");
        sb.append("run nohup bin/start").append("\n");
        sb.append("echo STARTED").append("\n");
        return sb.toString();
    }


    /**
     * Creates a shell script for starting an existing remote container.
     * @param options
     * @return
     * @throws MalformedURLException
     */
    public static String buildStartScript(CreateContainerOptions options) throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        sb.append(RUN_FUCNTION).append("\n");
        sb.append("run cd ~/containers/ ").append("\n");
        sb.append("run cd ").append(options.getName()).append("\n");
        sb.append("run cd `").append(FIRST_FABRIC_DIRECTORY).append("`\n");
        sb.append("run nohup bin/start").append("\n");
        return sb.toString();
    }

    /**
     * Creates a shell script for stopping a container.
     * @param options
     * @return
     * @throws MalformedURLException
     */
    public static String buildStopScript(CreateContainerOptions options) throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        sb.append(RUN_FUCNTION).append("\n");
        sb.append("run cd ~/containers/ ").append("\n");
        sb.append("run cd ").append(options.getName()).append("\n");
        sb.append("run cd `").append(FIRST_FABRIC_DIRECTORY).append("`\n");
        sb.append("run bin/stop").append("\n");
        return sb.toString();
    }

    /**
     * Creates a shell script for uninstalling a container.
     * @param options
     * @return
     * @throws MalformedURLException
     */
    public static String buildUninstallScript(CreateContainerOptions options) throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        sb.append(RUN_FUCNTION).append("\n");
        sb.append("run cd ~/containers/ ").append("\n");
        sb.append("run rm -rf ").append(options.getName()).append("\n");
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
        sb.append(String.format(LINE_APPEND,pattern.replaceAll("/","\\\\/"),line.replaceAll("/","\\\\/"),path,path+".tmp")).append("\n");
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

    private static void extractTargzIntoDirectory(StringBuilder sb, URI proxy, String groupId, String artifactId, String version) throws URISyntaxException {

        String file = artifactId + "-" + version + ".tar.gz";
        String directory =  groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version + "/";
        String artifactParentUri = proxy.resolve(directory).toString();
        String artifactUri = proxy.resolve(directory+file).toString();

        //TODO: There may be cases where this is not good enough
        String installationFolder = artifactId + "-" + version;

        //To cover the case of SNAPSHOT dependencies where URL can't be determined we are querying for the available versions first
        if(version.contains("SNAPSHOT")) {
            sb.append("run export DISTRO_URL=`curl --silent ").append(artifactParentUri).append("| grep href | grep \"tar.gz\\\"\" | sed 's/^.*<a href=\"//' | sed 's/\".*$//'  | tail -1`").append("\n");
        } else {
            sb.append("run export DISTRO_URL=`").append(artifactUri).append("`").append("\n");
        }
        sb.append("if [[  \"$DISTRO_URL\" == \"\" ]] ;  then export DISTRO_URL=").append(artifactUri).append("; fi\n");
        sb.append("download ").append("$DISTRO_URL").append(" ").append(file).append("\n");
        for (String fallbackRepo : FALLBACK_REPOS) {
            URI fallbackRepoURI = new URI(fallbackRepo);
            String fallbackDistro = fallbackRepoURI.resolve(directory + file).toString();
            sb.append("if [ ! -f " + file + " ] ; then ").append("download ").append(fallbackDistro).append(" ").append(file).append(" ; fi \n");
        }
        sb.append("run tar -xpzf ").append(file).append("\n");
    }

    private static String loadFunction(String function) {
        InputStream is = ContainerProviderUtils.class.getResourceAsStream(function);
        InputStreamReader reader = null;
        BufferedReader bufferedReader = null;
        StringBuilder sb = new StringBuilder();

        try {
            reader = new InputStreamReader(is, "UTF-8");
            bufferedReader = new BufferedReader(reader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (Throwable e) {
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (Throwable e) {
            }
            try {
                if (bufferedReader != null)
                    bufferedReader.close();
            } catch (Throwable e) {
            }
            try {
                is.close();
            } catch (Throwable e) {
            }

        }
        return sb.toString();
    }
}