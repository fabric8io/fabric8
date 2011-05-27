/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service.ssh;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.fusesource.fabric.api.AgentProvider;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.maven.MavenProxy;

public class SshAgentProvider implements AgentProvider {

    private MavenProxy mavenProxy;

    public void setMavenProxy(MavenProxy mavenProxy) {
        this.mavenProxy = mavenProxy;
    }

    @Override
    public void create(URI agentUri, String name, String zooKeeperUrl) {
        try {
            String script = buildStartupScript(mavenProxy.getAddress(), name, agentUri.getPath(), zooKeeperUrl);
            String host = agentUri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("host name must be specified in uri '" + agentUri + "'");
            }
            int port = agentUri.getPort();
            if (port == -1) {
                port = 22;
            }
            String ui = agentUri.getUserInfo();
            String[] uip = ui != null ? ui.split(":") : null;
            if (uip == null || uip.length != 2) {
                throw new IllegalArgumentException("user and password must be supplied in the uri '" + agentUri + "'");
            }
            sendScript(host, port, uip[0], uip[1], script, 6, 1);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    protected void sendScript(String host, int port, String username, String password, String script, int sshRetries, long retryDelay) throws Exception {
        Session session = null;
        Exception connectException = null;
        for (int i = 0; i < sshRetries; i++) {
            if (i > 0) {
                long delayMs = (long) (200L * Math.pow(i, 2));
                Thread.sleep(delayMs);
            }
            try {
                session = new JSch().getSession(username, host, port);
                session.setTimeout(60000);
                session.setPassword(password);
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();
                connectException = null;
                break;
            } catch (Exception from) {
                connectException = from;
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
                session = null;
            }
        }
        if (connectException != null) {
            throw connectException;
        }
        ChannelExec executor = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        try {
            executor = (ChannelExec) session.openChannel("exec");
            executor.setPty(true);
            executor.setCommand(script);
            executor.setOutputStream(output);
            executor.setErrStream(error);
            executor.connect();
            int errorStatus = -1;
            for (int i = 0; i < sshRetries; i++) {
                if (i > 0) {
                    long delayMs = (long) (200L * Math.pow(i, 2));
                    Thread.sleep(delayMs);
                }
                if ((errorStatus = executor.getExitStatus()) != -1) {
                    break;
                }
            }
            if (errorStatus == -1) {
                throw new Exception(String.format("%s@%s:%d: received exit status %d executing \n--- command ---\n%s\n--- output ---\n%s\n--- error ---\n%s\n------\n", username, host,
                        port, executor.getExitStatus(), script, output.toString(), error.toString()));
            }
        } finally {
            if (executor != null) {
                executor.disconnect();
            }
            session.disconnect();
        }
    }

    private String buildStartupScript(URI proxy, String name, String path, String zooKeeperUrl) throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        if (path.startsWith("/~/")) {
            path = path.substring(3);
        }
        sb.append("function run { echo \"Running: $*\" ; $* ; rc=$? ; if [ \"${rc}\" -ne 0 ]; then echo \"Command failed\n\" ; exit ${rc} ; fi ; }\n");
        sb.append("run cd").append("\n");
        sb.append("run pwd").append("\n");
        sb.append("run mkdir -p ").append(path).append("\n");
        sb.append("run cd ").append(path).append("\n");
        sb.append("run mkdir -p ").append(name).append("\n");
        sb.append("run cd ").append(name).append("\n");
        extractTargzIntoDirectory(sb, proxy, "org.apache.karaf", "apache-karaf", "2.2.1");
        sb.append("run cd ").append("apache-karaf-2.2.1").append("\n");
        List<String> lines = new ArrayList<String>();
        lines.add(downloadAndStartMavenBundle(sb, proxy, "org.fusesource.fabric", "fabric-linkedin-zookeeper", "1.0-SNAPSHOT", "jar") + "=60");
        lines.add(downloadAndStartMavenBundle(sb, proxy, "org.fusesource.fabric", "fabric-zookeeper", "1.0-SNAPSHOT", "jar") + "=60");
        lines.add(downloadAndStartMavenBundle(sb, proxy, "org.fusesource.fabric", "fabric-configadmin", "1.0-SNAPSHOT", "jar") + "=60");
        lines.add(downloadAndStartMavenBundle(sb, proxy, "org.fusesource.fabric", "fabric-agent", "1.0-SNAPSHOT", "jar") + "=60");
        appendFile(sb, "etc/startup.properties", lines);
        appendFile(sb, "etc/system.properties", Arrays.asList("karaf.name = " + name, "zookeeper.url = " + zooKeeperUrl));
        sb.append("run bin/start").append("\n");
        return sb.toString();
    }

    private String downloadAndStartMavenBundle(StringBuilder sb, URI proxy, String groupId, String artifactId, String version, String type) {
        String path = groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version;
        String file = path + "/" + artifactId + "-" + version + "." + type;
        sb.append("run mkdir -p " + "system/").append(path).append("\n");
        sb.append("run curl --show-error --get --retry 20 --output system/").append(file).append(" ").append(proxy.resolve(file)).append("\n");
        return file;
    }

    private void appendFile(StringBuilder sb, String path, Iterable<String> lines) {
        final String MARKER = "END_OF_FILE";
        sb.append("cat >> ").append(path).append(" <<'").append(MARKER).append("'\n");
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        sb.append(MARKER).append("\n");
    }

    private void extractTargzIntoDirectory(StringBuilder sb, URI proxy, String groupId, String artifactId, String version) {
        String file = artifactId + "-" + version + ".tar.gz";
        String path = groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version + "/" + file;
        sb.append("run curl --show-error --get --retry 20 --output ").append(file).append(" ").append(proxy.resolve(path)).append("\n");
        sb.append("run tar -xpzf ").append(file).append("\n");
    }

}
