package io.fabric8.maven;

import io.fabric8.api.jmx.MetaTypeObjectSummaryDTO;
import io.fabric8.common.util.Files;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;
import org.apache.felix.metatype.OCD;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
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

    @Parameter(property = "inputDir", defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}/system")
    private File inputDir = new File("target/system");

    @Parameter(property = "outputDir", defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}/metatype")
    private File outputDir = new File("target/metatype");

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            throw new MojoExecutionException("No input folder " + inputDir.getPath());
        }
        outputDir.mkdirs();
        processFolder(inputDir);
    }

    protected void processFolder(File input) throws MojoExecutionException {
        if (input.isDirectory()) {
            File[] files = input.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".jar")) {
                        try {
                            processJar(file);
                        } catch (IOException e) {
                            throw new MojoExecutionException("Failed to process jar " + file + ". " + e, e);
                        }
                    } else if (file.isDirectory()) {
                        processFolder(file);
                    }
                }
            }
        }
    }

    protected void processJar(File file) throws IOException {
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

    }

    protected void writeMetaTypeObjects(MetaData metadata, Properties properties, JarFile jarFile, JarEntry jarEntry) throws IOException {
        Map<String,Object> objectClassDefinitions = metadata.getObjectClassDefinitions();
        Set<Map.Entry<String, Object>> entries = objectClassDefinitions.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String pid = entry.getKey();
            Object value = entry.getValue();

            File pidOutDir = new File(outputDir, pid);
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
