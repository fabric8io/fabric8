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
package org.fusesource.patch.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;

import jline.console.ConsoleReader;
import jline.internal.ShutdownHooks;

import org.apache.felix.utils.version.VersionCleaner;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.fusesource.jansi.AnsiConsole;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Main {

    public static final String REPOSITORIES = "repositories";
    public static final String ARTIFACTS = "artifacts";
    public static final String CACHE = "cache";

    public static final String DEFAULT_REPOSITORIES = "https://repo.fusesource.com/nexus/content/repositories/releases/," +
                                                      "https://repo.fusesource.com/nexus/content/groups/ea/";
    
    public static final String DEFAULT_ARTIFACTS = 
            //"org.apache.felix:org.apache.felix.framework," +
            "org.apache.felix:org.apache.felix.configadmin," +
            "org.apache.felix:org.apache.felix.eventadmin," +
            "org.apache.felix:org.apache.felix.fileinstall," +
            "org.apache.felix:org.apache.felix.webconsole," +
            "org.apache.aries.blueprint:blueprint," +
            "org.apache.aries.jmx:jmx," +
            "org.apache.aries:org.apache.aries.util," +
            "org.apache.aries.transaction:transaction," +
            "org.apache.servicemix.specs:specs," +
            "org.apache.karaf:karaf," +
            "org.apache.cxf:cxf," +
            "org.apache.camel:camel," +
            "org.apache.activemq:activemq-parent," +
            "org.apache.servicemix:servicemix-utils," +
            "org.apache.servicemix:components," +
            "org.apache.servicemix.nmr:nmr-parent," +
            "org.apache.servicemix:features," +
            "org.apache.servicemix:archetypes," +
            "org.fusesource:fuse-project";

    public static final String DEFAULT_CACHE = "data/patches";

    private final ExecutorService executor;
    private final List<String> repos;
    private final List<String> artifacts;
    private final File cache;
    private final ConsoleReader console;

    public Main(List<String> repos, List<String> artifacts, File cache) throws IOException {
        this.executor = Executors.newFixedThreadPool(50);
        this.repos = repos;
        this.artifacts = artifacts;
        this.cache = cache;
        this.cache.mkdirs();
        this.console = new ConsoleReader(System.in, System.out);
    }

    public void run() throws Exception {
        ConsoleReader reader = new ConsoleReader(System.in, System.out);

        System.out.println("Checking repositories ...");
        final List<Patch> patches = getPossiblePatches();
        System.out.println("Downloading patch metadatas ...");
        downloadPatchMetadata(patches, cache);
        System.out.println("Downloading missing patches in the background ...");
        downloadPatches(patches, cache);

        while (true) {
            System.out.println();
            System.out.println("Available patches:");
            printPatches(patches);
            System.out.println("Which patches do you want to apply?");
            System.out.println("  Specify one or more patch or wildcard expressions separated by spaces.");
            System.out.println("  Full syntax for a patch is [artifact]/[version]");
            System.out.println("  Enter 'exit' to abort.");
            String line;
            do {
                line = console.readLine("> ");
            } while (line == null || line.isEmpty());
            if (line.trim().toLowerCase().equals("exit")) {
                return;
            }

            List<Patch> toInstall = new ArrayList<Patch>();
            List<Pattern> patterns = new ArrayList<Pattern>();
            for (String wildcard : line.split(" ")) {
                patterns.add(Pattern.compile(wildcardToRegex(wildcard)));
            }
            for (Patch patch : patches) {
                if (patch.metadata != null) {
                    String id = patch.artifact + "/" + patch.version;
                    for (Pattern pattern : patterns) {
                        if (pattern.matcher(id).matches()) {
                            toInstall.add(patch);
                            break;
                        }
                    }
                }
            }
            if (toInstall.isEmpty()) {
                System.out.println("No matching patches");
            } else {
                System.out.println("List of patches to install:");
                printPatches(toInstall);
                line = console.readLine("Do you want to install these patches (yes/no): ");
                if (line == null || !line.trim().toLowerCase().equals("yes")) {
                    System.out.println("Aborting ...");
                } else {
                    System.out.println("Installing patches ...");
                    install(toInstall);
                }
            }
        }
    }

    protected String getVersion() {
        Properties props = new Properties();
        InputStream is = getClass().getResourceAsStream("version.properties");
        try {
            props.load(is);
        } catch (Exception e) {
            // Ignore
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        return props.getProperty("version");
    }

    protected void install(List<Patch> patches) throws Exception {
        boolean finished = true;
        for (Patch patch : patches) {
            finished &= patch.patchFile.isDone();
        }
        if (!finished) {
            System.out.println("Waiting for downloads to finish ...");
            for (Patch patch : patches) {
                patch.patchFile.get();
            }
        }

        System.out.println("Connecting to the local instance:");
        String host = "localhost"; //readLine("Host", "localhost");
        String port = readLine("Port", "8101");
        String user = readLine("User", "admin");
        String pass = readLine("Password", "admin", true);

        StringBuilder sb = new StringBuilder();
        sb.append("Installing patches on local instance (port ").append(port).append("): ");
        boolean first = true;
        for (Patch p : patches) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(p.metadata.getProperty("id"));
        }
        audit(sb.toString());


        String version = getVersion();
        String osgiVersion = VersionCleaner.clean(version);

        String script =
                "# Helper methods\n" +
                "or = { _v = false ; each ( \"$args\" split \" \" ) { if { new java.lang.Boolean \"$it\" } { _v = true } } ; $_v }\n" +
                "and = { _v = true ; each ( \"$args\" split \" \" ) { if { new java.lang.Boolean \"$it\" } { } { _v = false } } ; $_v }\n" +
                "not = { _v1 = $1 ; if { $_v1 } { false } { true } }\n" +
                "lesser = { if { (($1 compareTo $2) compareTo (new java.lang.Integer 0)) equals (new java.lang.Integer -1) } { true } { false } }\n" +
                "# Check if the patch-core bundle is installed\n" +
                "bundle = null\n" +
                "found = false\n" +
                "each  ($.context bundles) { \n" +
                "  if  { ($it symbolicName) equals org.fusesource.patch.patch-core } { \n" +
                "    bundle = $it\n" +
                "    found = true \n" +
                "  }\n" +
                "}\n" +
                "# Install patch-core bundle if not already available\n" +
                "if { $found } {\n" +
                "  v1 = ($bundle version)\n" +
                "  v2 = (new org.osgi.framework.Version " + osgiVersion + ")\n" +
                "  if { or ( lesser ($v1 major) ($v2 major) ) " +
                "          ( lesser ($v1 minor) ($v2 minor) ) " +
                "          ( lesser ($v1 micro) ($v2 micro) ) " +
                "          ( ($v2 qualifier) endsWith \"SNAPSHOT\" ) } {\n" +
                "    echo 'An older patch-core bundle has been found, installing a newer one.'\n" +
                "    $bundle uninstall \n" +
                "    bundle = ($.context installBundle mvn:org.fusesource.patch/patch-core/" + version + ")\n" +
                "    $bundle start\n" +
                "  } {\n" +
                "    echo 'Found an up-to-date patch-core bundle.'\n" +
                "  }\n" +
                "} {\n" +
                "    echo 'Installing patch-core bundle.'\n" +
                "  bundle = ($.context installBundle mvn:org.fusesource.patch/patch-core/" + version + ")\n" +
                "  $bundle start\n" +
                "}\n" +
                "# Create patch service\n" +
                "service = (new ($bundle loadClass org.fusesource.patch.impl.ServiceImpl) $.context)\n";
        for (Patch patch : patches) {
            script += "$service download (new java.net.URL " + patch.patchFile.get().toURI().toURL().toExternalForm() + ")\n";
        }
        script += "echo 'Installing patches ...'\n";
        script += "$service cliInstall ";
        for (Patch patch : patches) {
            script += patch.metadata.getProperty("id") + " ";
        }

        SshClient client = null;
        try {
            client = SshClient.setUpDefaultClient();
            client.start();
            ConnectFuture future = client.connect(host, Integer.parseInt(port));
            ClientSession session = future.await().getSession();
            if (!session.authPassword(user, pass).await().isSuccess()) {
                System.err.println("Authentication failure, aborting.");
                return;
            }
            ClientChannel channel = session.createChannel("exec", script);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            channel.setIn(new ByteArrayInputStream(new byte[0]));
            channel.setOut(out);
            channel.setErr(err);
            channel.open();
            channel.waitFor(ClientChannel.CLOSED, 0);
            System.out.println(out.toString());
            System.err.println(err.toString());
        } finally {
            try {
                client.stop();
            } catch (Throwable t) { }
        }
    }

    private String readLine(String msg, String def) throws IOException {
        return readLine(msg, def, false);
    }

    private String readLine(String msg, String def, boolean password) throws IOException {
        console.setEchoCharacter(password ? '*' : null);
        String val = console.readLine(msg + " (defaults to " + def + "): ");
        if (val == null || val.isEmpty()) {
            val = def;
        }
        return val;
    }

    private void printPatches(List<Patch> patches) {
        Map<String, Map<String, Patch>> patchMap = new TreeMap<String, Map<String, Patch>>();
        for (Patch patch : patches) {
            if (patch.metadata != null) {
                Map<String, Patch> versions = patchMap.get(patch.artifact);
                if (versions == null) {
                    versions = new TreeMap<String, Patch>();
                    patchMap.put(patch.artifact, versions);
                }
                versions.put(patch.version, patch);
            }
        }
        for (Map.Entry<String, Map<String, Patch>> artifactEntry : patchMap.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append("    ");
            sb.append(artifactEntry.getKey());
            while (sb.length() < 54) {
                sb.append(' ');
            }
            boolean first = true;
            for (String v : artifactEntry.getValue().keySet()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(v);
            }
            System.out.println(sb.toString());
        }
    }

    public void audit(String message) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("patch-log.txt", true));
            try {
                out.write(DateFormat.getDateTimeInstance().format(new Date()));
                out.write(" | ");
                out.write(message);
                out.write("\n");
            } finally {
                out.close();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    public static String wildcardToRegex(String wildcard){
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch(c) {
                case '*':
                    s.append(".*");
                    break;
                case '?':
                    s.append(".");
                    break;
                // escape special regexp-characters
                case '(': case ')': case '[': case ']': case '$':
                case '^': case '.': case '{': case '}': case '|':
                case '\\':
                    s.append("\\");
                    s.append(c);
                    break;
                default:
                    s.append(c);
                    break;
            }
        }
        s.append('$');
        return(s.toString());
    }

    public void downloadPatchMetadata(final List<Patch> locations, final File directory) throws InterruptedException {
        final Properties ids = new Properties();
        File idsFile = new File(directory, "ids.txt");
        if (idsFile.isFile()) {
            try {
                FileInputStream fis = new FileInputStream(idsFile);
                try {
                    ids.load(fis);
                } finally {
                    fis.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        final CountDownLatch latch = new CountDownLatch(locations.size());
        for (final Patch patch : locations) {
            final String id;
            synchronized (ids) {
                id = ids.getProperty(patch.artifact + ":" + patch.version, patch.location.getPath());
            }
            final URL location = patch.location;
            final File file = new File(directory, new File(id + ".patch").getName());
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        download(file, location);
                        patch.metadataFile = file;
                        Properties props = new Properties();
                        FileInputStream fis = new FileInputStream(file);
                        try {
                            props.load(fis);
                        } finally {
                            fis.close();
                        }
                        patch.metadata = props;
                        String patchId = patch.metadata.getProperty("id");
                        if (patchId != null && !file.getName().equals(patchId + ".patch")) {
                            file.renameTo(new File(directory, patchId + ".patch"));
                            synchronized (ids) {
                                ids.setProperty(patch.artifact + ":" + patch.version, patchId);
                            }
                        }
                    } catch (FileNotFoundException e) {
                        // Ignore
                    } catch (Exception e) {
                        System.err.println("Error downloading " + location + ": " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        latch.await();
        try {
            FileOutputStream fos = new FileOutputStream(idsFile);
            try {
                ids.store(fos, "");
            } finally {
                fos.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private void download(File file, URL location) throws IOException {
        if (!file.isFile()) {
            File temp = new File(file.toString() + ".tmp");
            URLConnection con = location.openConnection();
            if (location.getUserInfo() != null) {
                con.setRequestProperty("Authorization", "Basic " + new sun.misc.BASE64Encoder().encode(location.getUserInfo().getBytes()));
            }
            if (temp.isFile()) {
                con.setRequestProperty("Range", "Bytes=" + (temp.length()) + "-");
            }
            InputStream is = new BufferedInputStream(con.getInputStream());
            try {
                boolean resume = "bytes".equals(con.getHeaderField("Accept-Ranges"));
                OutputStream os = new BufferedOutputStream(new FileOutputStream(temp, resume));
                try {
                    copy(is, os);
                } finally {
                    os.close();
                }
                temp.renameTo(file);
            } finally {
                is.close();
            }
        }
    }

    private void copy(InputStream is, OutputStream os) throws IOException {
        try {
            byte[] b = new byte[4096];
            int l = is.read(b);
            while (l >= 0) {
                os.write(b, 0, l);
                l = is.read(b);
            }
        } finally {
            os.close();
        }
    }

    public void downloadPatches(final List<Patch> locations, final File directory) throws InterruptedException, MalformedURLException {
        for (final Patch patch : locations) {
            if (patch.metadata == null) {
                continue;
            }
            final URL location = patch.location;
            final URL patchurl = new URL(location.toExternalForm().replaceAll("-patch.patch", "-patch.zip"));
            final File file = new File(directory, patch.metadata.getProperty("id") + ".zip");
            if (!file.isFile()) {
                patch.patchFile = executor.submit(new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        download(file, patchurl);
                        return file;
                    }
                });
            } else {
                patch.patchFile = new Future<File>() {
                    @Override
                    public boolean cancel(boolean mayInterruptIfRunning) {
                        return false;
                    }
                    @Override
                    public boolean isCancelled() {
                        return false;
                    }
                    @Override
                    public boolean isDone() {
                        return true;
                    }
                    @Override
                    public File get() throws InterruptedException, ExecutionException {
                        return file;
                    }
                    @Override
                    public File get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                        return file;
                    }
                };
            }
        }
    }

    public List<Patch> getPossiblePatches() throws InterruptedException {
        final List<Patch> patches = new ArrayList<Patch>();
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final CountDownLatch latch = new CountDownLatch(repos.size() * artifacts.size());
        for (final String repo : repos) {
            for (final String artifact : artifacts) {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String[] mvn = artifact.split(":");
                            URL base = new URL(repo + mvn[0].replace('.', '/') + "/" + mvn[1] + "/");
                            URL metadata = new URL(base, "maven-metadata.xml");
                            URLConnection con = metadata.openConnection();
                            if (metadata.getUserInfo() != null) {
                                con.setRequestProperty("Authorization", "Basic " + new sun.misc.BASE64Encoder().encode(metadata.getUserInfo().getBytes()));
                            }
                            InputStream is = con.getInputStream();
                            try {
                                Document doc = dbf.newDocumentBuilder().parse(is);
                                NodeList versions = doc.getDocumentElement().getElementsByTagName("version");
                                for (int i = 0; i < versions.getLength(); i++) {
                                    Node version = versions.item(i);
                                    String v = version.getTextContent();
                                    URL p = new URL(base, v + "/" + mvn[1] + "-" + v + "-patch.patch");
                                    synchronized (patches) {
                                        Patch patch = new Patch();
                                        patch.artifact = artifact;
                                        patch.repo = repo;
                                        patch.version = v;
                                        patch.location = p;
                                        patches.add(patch);
                                    }
                                }
                            } finally {
                                is.close();
                            }
                        } catch (FileNotFoundException e) {
                            // Ignore
                        } catch (Exception e) {
                            System.err.println("Error in " + repo + artifact + ": " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
        }
        latch.await();
        return patches;
    }

    public void destroy() {
        try {
            console.getTerminal().restore();
        } catch (Throwable e) {
            // Ignore
        }
        executor.shutdown();
    }

    public static class Patch {
        String artifact;
        String version;
        String repo;
        URL location;
        File metadataFile;
        Properties metadata;
        Future<File> patchFile;
    }

    public static void main(String[] args) throws Exception {
        System.setProperty(ShutdownHooks.JLINE_SHUTDOWNHOOK, "true");
        AnsiConsole.systemInstall();

        for (String arg : args) {
            int idx = arg.indexOf('=');
            if (idx > 0) {
                System.setProperty(arg.substring(0, idx), arg.substring(idx + 1));
            }
        }

        List<String> repos = new ArrayList<String>();
        String repoStr = System.getProperty(REPOSITORIES, DEFAULT_REPOSITORIES);
        for (String repo : repoStr.split(",")) {
            repo = repo.trim();
            if (!repo.endsWith("/")) {
                repo += "/";
            }
            repos.add(repo);
        }
        List<String> artifacts = new ArrayList<String>();
        String artifactStr = System.getProperty(ARTIFACTS, DEFAULT_ARTIFACTS);
        for (String artifact : artifactStr.split(",")) {
            artifact = artifact.trim();
            artifacts.add(artifact);
        }
        File cache = new File(System.getProperty(CACHE, DEFAULT_CACHE));

        Main main = new Main(repos, artifacts, cache);
        try {
            main.run();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            main.destroy();
        }
    }

}
