package io.fabric8.patch.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A few useful methods for testing the patching mechanism
 */
public abstract class PatchTestSupport {

    protected static File getTestResourcesDirectory() {
        return getDirectoryForResource("log4j.properties");
    }

    /*
     * Get the directory where a test resource has been stored
     */
    protected static File getDirectoryForResource(String name) {
        return getFileForResource(name).getParentFile();
    }

    /*
     * Get the File object for a test resource
     */
    protected static File getFileForResource(String name) {
        URL base = PatchTestSupport.class.getClassLoader().getResource(name);
        try {
            return new File(base.toURI());
        } catch(URISyntaxException e) {
            return new File(base.getPath());
        }
    }

    protected static File createKarafDirectory(File basedir) throws Exception {
        File result = new File(basedir, "karaf");
        delete(result);
        result.mkdirs();
        new File(result, "etc").mkdir();
        new File(result, "etc/startup.properties").createNewFile();
        new File(result, "bin").mkdir();
        FileUtils.write(new File(result, "bin/karaf"), "This is the original bin/karaf file");
        System.setProperty("karaf.base", result.getAbsolutePath());
        System.setProperty("karaf.home", result.getAbsolutePath());
        return result;
    }

    protected static void  delete(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete( child );
            }
            file.delete();
        } else if (file.isFile()) {
            file.delete();
        }
    }

    protected static PatchBuilder patch(String name) throws Exception {
        return new PatchBuilder(getFileForResource(name));
    }

    protected static final class PatchBuilder {

        private final PatchData data;
        private final File file;
        private final ZipOutputStream zip;

        private PatchBuilder(File patch) throws IOException {
            data = PatchData.load(new FileInputStream(patch));

            File workdir = new File("target/generated-patches");
            workdir.mkdirs();
            file = new File(workdir, String.format("%s-%tQ.zip", data.getId(), new Date()));

            zip = new ZipOutputStream(new FileOutputStream(file));
            zip.putNextEntry(new ZipEntry(patch.getName()));
            IOUtils.copy(new FileInputStream(patch), zip);
            zip.closeEntry();
        }

        protected PatchBuilder withFile(String name) throws Exception {
            ZipEntry entry = new ZipEntry(name);
            zip.putNextEntry(entry);
            IOUtils.write("Some random data goes here", zip);
            zip.closeEntry();
            return this;
        }

        protected File build() throws Exception {
            // let's make sure all necessary files are in the zip
            for (String file : data.getFiles()) {
                withFile(file);
            }

            zip.flush();
            zip.close();
            return file;
        }
    }
}
