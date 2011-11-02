/*
 * Copyright (C) 2011 FuseSource, Corp. All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the CDDL license
 * a copy of which has been included with this distribution in the license.txt file.
 */

package org.fusesource.fabric.internal;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AgentProviderUtils {

    private static final String REPLACE_FORMAT = "sed -i  \"s/%s/%s/g\" %s";

    public static final int DEFAULT_SSH_PORT = 8081;

    private AgentProviderUtils() {
        //Utility Class
    }

    public static String buildStartupScript(URI proxy, String name, String path,  String zooKeeperUrl, int sshPort, boolean debugAgent) throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        sb.append("function run { echo \"Running: $*\" ; $* ; rc=$? ; if [ \"${rc}\" -ne 0 ]; then echo \"Command failed\" ; exit ${rc} ; fi ; }\n");
        sb.append("run mkdir -p ").append(name).append("\n");
        sb.append("run cd ").append(name).append("\n");
        extractTargzIntoDirectory(sb, proxy, "org.apache.karaf", "apache-karaf", "2.2.0-fuse-00-43");
        sb.append("run cd ").append("apache-karaf-2.2.0-fuse-00-43").append("\n");
        List<String> lines = new ArrayList<String>();

        //TODO: Need to find a more elegant way to define the bundles.
        lines.add(downloadAndStartMavenBundle(sb, proxy, "org.codehaus.jackson", "jackson-core-asl", "1.8.4", "jar") + "=60");
        lines.add(downloadAndStartMavenBundle(sb, proxy, "org.codehaus.jackson", "jackson-mapper-asl", "1.8.4", "jar") + "=60");
        lines.add(downloadAndStartMavenBundle(sb, proxy, "org.codehaus.jackson", "jackson-mapper-asl", "1.8.4", "jar") + "=60");

        lines.add(downloadAndStartMavenBundle(sb, proxy, "org.fusesource.fabric", "fabric-linkedin-zookeeper", "1.1-SNAPSHOT", "jar") + "=60");
        lines.add(downloadAndStartMavenBundle(sb, proxy, "org.fusesource.fabric", "fabric-zookeeper", "1.1-SNAPSHOT", "jar") + "=60");
        lines.add(downloadAndStartMavenBundle(sb, proxy, "org.fusesource.fabric", "fabric-configadmin", "1.1-SNAPSHOT", "jar") + "=60");
        lines.add(downloadAndStartMavenBundle(sb, proxy, "org.fusesource.fabric", "fabric-agent", "1.1-SNAPSHOT", "jar") + "=60");

        appendFile(sb, "etc/startup.properties", lines);
        replaceLineInFile(sb,"etc/system.properties","karaf.name=root","karaf.name = "+name);
        replaceLineInFile(sb,"etc/org.apache.karaf.shell.cfg","sshPort=8101","sshPort="+sshPort);
        appendFile(sb, "etc/system.properties", Arrays.asList("zookeeper.url = " + zooKeeperUrl));
        if(debugAgent) {
           sb.append("run export KARAF_DEBUG=true").append("\n");
        }
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
        final String MARKER = "END_OF_FILE";
        sb.append(String.format(REPLACE_FORMAT,pattern,line,path)).append("\n");
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
        String path = groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version + "/" + file;
        sb.append("run curl --show-error --silent --get --retry 20 --output ").append(file).append(" ").append(proxy.resolve(path)).append("\n");
        sb.append("run tar -xpzf ").append(file).append("\n");
    }
}
