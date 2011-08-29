/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.maven.impl;

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.fusesource.fabric.maven.MavenProxy;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MavenProxyImpl implements MavenProxy {

    private static final Logger LOGGER = Logger.getLogger(MavenProxyImpl.class.getName());

    private int port = 8040;
    private String localRepository;
    private String remoteRepositories = "repo1.maven.org/maven2,repo.fusesource.com/nexus/content/groups/public";

    private List<RemoteRepository> repositories;
    private ServerSocket serverSocket;
    private RepositorySystem system;
    private RepositorySystemSession session;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getLocalRepository() {
        return localRepository;
    }

    public void setLocalRepository(String localRepository) {
        this.localRepository = localRepository;
    }

    public String getRemoteRepositories() {
        return remoteRepositories;
    }

    public void setRemoteRepositories(String remoteRepositories) {
        this.remoteRepositories = remoteRepositories;
    }

    public synchronized URI getAddress() {
        if (serverSocket != null) {
            return URI.create("http://" + getLocalHostName() + ":" + serverSocket.getLocalPort() + "/");
        } else {
            return null;
        }
    }

    public synchronized boolean isStarted() {
        return serverSocket != null;
    }


    public synchronized void start() throws IOException {
        if (port >= 0) {
            if (localRepository.equals("")) {
                localRepository = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
            }
            if (system == null) {
                system = newRepositorySystem();
            }
            if (session == null) {
                session = newSession( system, localRepository );
            }
            repositories = new ArrayList<RemoteRepository>();
            int i = 0;
            for (String rep : remoteRepositories.split(",")) {
                repositories.add(new RemoteRepository( "repo-" + i++, "default", rep ));
            }

            String repos = "local:" + localRepository + " ";
            for (RemoteRepository repo : repositories) {
                repos += repo + " ";
            }
            repos = repos.trim();

            serverSocket = new ServerSocket(port);
            new Acceptor(serverSocket).start();
            LOGGER.log(Level.INFO, String.format("Maven proxy started at address : %s with configured repositories : %s", getAddress(), repos));
        }
    }

    public synchronized void stop() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
//                System.out.println("Maven proxy stopped");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                serverSocket = null;
            }
        }
    }

    protected class Acceptor extends Thread {

        private final ServerSocket serverSocket;

        public Acceptor(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public void run() {
            try {
                while (!serverSocket.isClosed()) {
                    Socket sock = serverSocket.accept();
                    new Worker(sock).start();
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Exception caught in maven proxy", e);
            }
        }

    }

    protected class Worker extends Thread {

        private final Socket socket;

        public Worker(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            InputStream inputStream = null;
            BufferedOutputStream output = null;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new BufferedOutputStream(socket.getOutputStream());
                String headLine = reader.readLine();
                if (headLine == null || !headLine.startsWith("GET ") || !headLine.endsWith(" HTTP/1.0") && !headLine.endsWith(" HTTP/1.1")) {
                    LOGGER.log(Level.WARNING, String.format("Received invalid method : %s", headLine));
                    output.write("HTTP/1.0 405 Invalid method.\r\n\r\n".getBytes());
                    return;
                }

                String path = headLine.substring(4, headLine.length() - 9);
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                String mvn = convertToMavenUrl(path);
                LOGGER.log(Level.INFO, String.format("Received request for file : %s", mvn));

                try {
                    Artifact artifact = new DefaultArtifact( mvn, null );
                    ArtifactRequest request = new ArtifactRequest( artifact, repositories, null );
                    ArtifactResult result = system.resolveArtifact( session, request );

                    inputStream = new FileInputStream(result.getArtifact().getFile());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, String.format("Could not find file : %s due to %s", mvn, e));
                    output.write("HTTP/1.0 404 File not found.\r\n\r\n".getBytes());
                    return;
                }

                LOGGER.log(Level.INFO, String.format("Writing response for file : %s", mvn));
                output.write(("HTTP/1.1 200 OK\r\n"
                        + "Date: " + (new Date()).toString() + "\r\n"
                        + "Server: FON Proxy/" + "1.0-SNAPSHOT" + "\r\n"
                        + "Connection: close\r\n"
                        + "Content-Type: application/octet-stream\r\n"
                        + "Content-Length: " + inputStream.available() + "\r\n"
                        + "\r\n"
                ).getBytes());

                byte buffer[] = new byte[4096];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }

                output.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception caught in maven proxy", e);
            } finally {
                close(inputStream, output);
            }
        }


    }

    private RepositorySystemSession newSession( RepositorySystem system, String localRepository )
    {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        LocalRepository localRepo = new LocalRepository( localRepository );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( localRepo ) );
        return session;
    }

    private RepositorySystem newRepositorySystem()
    {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.setServices(WagonProvider.class, new ManualWagonProvider());
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        locator.setService(org.sonatype.aether.spi.log.Logger.class, LogAdapter.class);
        return locator.getService( RepositorySystem.class );
    }

    public static class ManualWagonProvider implements WagonProvider
    {

        public Wagon lookup( String roleHint )
            throws Exception
        {
            if( "file".equals( roleHint ) )
            {
                return new FileWagon();
            }
            else if( "http".equals( roleHint ) )
            {
                return new LightweightHttpWagon();
            }
            return null;
        }

        public void release( Wagon wagon )
        {

        }

    }

    public static class LogAdapter implements org.sonatype.aether.spi.log.Logger
    {

        public boolean isDebugEnabled()
        {
            return LOGGER.isLoggable( Level.FINE );
        }

        public void debug( String msg )
        {
            LOGGER.log(Level.FINE, msg);
        }

        public void debug( String msg, Throwable error )
        {
            LOGGER.log(Level.FINE, msg, error);
        }

        public boolean isWarnEnabled()
        {
            return LOGGER.isLoggable(Level.WARNING);
        }

        public void warn(String msg)
        {
            LOGGER.log( Level.WARNING, msg );
        }

        public void warn(String msg, Throwable error)
        {
            LOGGER.log( Level.WARNING, msg, error );
        }
    }

    private static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception exception) {
            }
        }
    }

    private static String convertToMavenUrl(String location) {
        String[] p = location.split("/");
        if (p.length >= 4 && p[p.length - 1].startsWith(p[p.length - 3] + "-" + p[p.length - 2])) {
            String artifactId = p[p.length - 3];
            String version = p[p.length - 2];
            String classifier;
            String type;
            String artifactIdVersion = artifactId + "-" + version;
            StringBuffer sb = new StringBuffer();
            if (p[p.length - 1].charAt(artifactIdVersion.length()) == '-') {
                classifier = p[p.length - 1].substring(artifactIdVersion.length() + 1, p[p.length - 1].lastIndexOf('.'));
                artifactIdVersion += "-" + classifier;
            } else {
                classifier = "";
            }
            type = p[p.length - 1].substring(artifactIdVersion.length() + 1);
            for (int j = 0; j < p.length - 3; j++) {
                if (j > 0) {
                    sb.append('.');
                }
                sb.append(p[j]);
            }
            sb.append(':').append(artifactId).append(':').append(type);
            if (classifier.length() > 0) {
                sb.append(":").append(classifier);
            }
            sb.append(":").append(version);
            return sb.toString();
        } else {
            return null;
        }
    }

    private static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to get address", e);
        }
    }

}
