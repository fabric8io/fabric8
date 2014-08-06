package io.fabric8.maven;

import io.fabric8.common.util.Files;
import io.fabric8.common.util.IOHelpers;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
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
    protected static String PROPERTIES_SUFFIX = ".properties";
    protected static String XML_SUFFIX = ".xml";

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
            Map<String,String> xmlMap = new HashMap<>();
            Map<String,MetaData> metadataMap = new HashMap<>();
            Map<String,Properties> propertiesMap = new HashMap<>();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("OSGI-INF/metatype/")) {
                    if (name.endsWith(XML_SUFFIX)) {
                        MetaDataReader reader = new MetaDataReader();
                        InputStream in = jarFile.getInputStream(entry);
                        if (in != null) {
                            String text = IOHelpers.readFully(in);
                            MetaData metadata = reader.parse(new ByteArrayInputStream(text.getBytes()));
                            if (metadata != null) {
                                String pid = name.substring(0, name.length() - XML_SUFFIX.length());
                                xmlMap.put(pid, text);
                                metadataMap.put(pid, metadata);
                            }
                        }
                    } else if (name.endsWith(PROPERTIES_SUFFIX)) {
                        String pid = name.substring(0, name.length() - PROPERTIES_SUFFIX.length());
                        Properties properties = new Properties();
                        InputStream in = jarFile.getInputStream(entry);
                        if (in != null) {
                            properties.load(in);
                            propertiesMap.put(pid, properties);
                        }
                    }
                }
            }
            Set<Map.Entry<String, MetaData>> metadataEntries = metadataMap.entrySet();
            for (Map.Entry<String, MetaData> metadataEntry : metadataEntries) {
                String pid = metadataEntry.getKey();
                MetaData metadata = metadataEntry.getValue();
                Properties properties = propertiesMap.get(pid);
                if (properties == null) {
                    properties = new Properties();
                }
                String xml = xmlMap.get(pid);
                if (xml == null) {
                   getLog().warn("Missing XML file for " + pid);
                } else {
                    writeMetaTypeObjects(metadata, properties, xml);
                }
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process jar " + file + ". " + e, e);
        }
    }

    protected void writeMetaTypeObjects(MetaData metadata, Properties properties, String xml) throws IOException {
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
            Files.writeToFile(xmlFile, xml.getBytes());
            if (properties.size() > 0) {
                File propertiesFile = new File(pidOutDir, "metatype.properties");
                properties.store(new FileOutputStream(propertiesFile), "Generated from jar by fabric8-maven-plugin");
            }
        }
    }

}
