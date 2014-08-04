package io.fabric8.maven;

import io.fabric8.common.util.Files;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Copies the OSGi MetaType metadata files from the projects dependencies to the give folder
 */
@Mojo(name = "copy-metatype")
public class CopyMetaTypeFilesMojo extends AbstractMojo {

    @Parameter(property = "inputDir", defaultValue = "${project.build.directory}/features-repo")
    private File inputDir = new File("target/system");

    @Parameter(property = "outputDir", defaultValue = "${project.build.directory}/metatype")
    private File outputDir = new File("target/metatype");

    @Component
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        outputDir.mkdirs();
        if (inputDir.exists() && inputDir.isDirectory()) {
            processFolder(inputDir);
        } else {
            getLog().warn("inputDir " + inputDir + " is not an existing directory");
        }

        // now lets process the classpath elements in case there's any extra
        processClassPath();
    }

    protected void processClassPath() throws MojoExecutionException {
        try {
            Set<String> elements = new HashSet<>();
            elements.addAll(project.getCompileClasspathElements());
            elements.addAll(project.getRuntimeClasspathElements());
            elements.addAll(project.getSystemClasspathElements());
            for (Object object : elements) {
                if (object != null) {
                    String path = object.toString();
                    File file = new File(path);
                    if (file.isFile() && isJar(file)) {
                        processJar(file);
                    }
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve classpath: " + e, e);
        }
    }

    protected static boolean isJar(File file) {
        return file.getName().endsWith(".jar");
    }

    protected void processFolder(File input) throws MojoExecutionException {
        if (input.isDirectory()) {
            File[] files = input.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && isJar(file)) {
                        processJar(file);
                    } else if (file.isDirectory()) {
                        processFolder(file);
                    }
                }
            }
        }
    }

    protected void processJar(File file) throws MojoExecutionException {
        try {
            JarFile jarFile = new JarFile(file);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("OSGI-INF/metatype/")) {
                    if (name.endsWith(".xml")) {
                        MetaDataReader reader = new MetaDataReader();
                        InputStream in = jarFile.getInputStream(entry);
                        if (in != null) {
                            MetaData metadata = reader.parse(in);

                            // lets try get the i18n properties
                            Properties properties = new Properties();
                            String propertiesFile = name.substring(0, name.length() - 3) + "properties";
                            ZipEntry propertiesEntry = jarFile.getEntry(propertiesFile);
                            if (propertiesEntry != null) {
                                InputStream propertiesIn = jarFile.getInputStream(entry);
                                if (propertiesIn != null) {
                                    properties.load(propertiesIn);
                                }
                            }
                            writeMetaTypeObjects(metadata, properties, jarFile, entry);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process jar " + file + ". " + e, e);
        }
    }

    protected void writeMetaTypeObjects(MetaData metadata, Properties properties, JarFile jarFile, JarEntry jarEntry) throws IOException {
        Map<String, Object> map = metadata.getDesignates();
        Set<Map.Entry<String, Object>> entries = map.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String pid = entry.getKey();
            File pidOutDir = new File(outputDir, pid);
            if (pidOutDir.exists()) {
                System.out.println("PID " + pid + " already exists at " + pidOutDir);
                return;
            }
            pidOutDir.mkdirs();
            File xmlFile = new File(pidOutDir, "metatype.xml");
            InputStream in = jarFile.getInputStream(jarEntry);
            Files.copy(in, new FileOutputStream(xmlFile));
            if (properties.size() > 0) {
                File propertiesFile = new File(pidOutDir, "metatype.properties");
                properties.store(new FileOutputStream(propertiesFile), "Generated from jar by fabric8-maven-plugin");
            }
        }
    }

}
