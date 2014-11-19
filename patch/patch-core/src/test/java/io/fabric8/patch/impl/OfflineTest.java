package io.fabric8.patch.impl;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for modifying files during patch apply/rollback
 */
public class OfflineTest extends PatchTestSupport {

    private File basedir;
    private File karaf;
    private Offline offline;

    @Before
    public void setup() throws Exception {
        basedir = PatchTestSupport.getTestResourcesDirectory();
        karaf = createKarafDirectory(basedir);
        offline = new Offline(karaf);
    }

    @Test
    public void testReplaceBinKaraf() throws Exception {
        assertTrue(new File(karaf, "bin/karaf").exists());

        long checksum = FileUtils.checksumCRC32(new File(karaf, "bin/karaf"));

        File patch = patch("files/files2.patch").build();
        offline.apply(patch);

        assertFalse("bin/karaf file should have changed", FileUtils.checksumCRC32(new File(karaf, "bin/karaf")) == checksum);

        offline.rollback(patch);
        assertEquals("bin/karaf file should have reverted back to the original file", checksum, FileUtils.checksumCRC32(new File(karaf, "bin/karaf")));
    }

    @Test
    public void testAddAndRemoveFile() throws Exception {
        File patched = new File(karaf, "bin/some-other-file");
        assertFalse(patched.exists());

        File patch = patch("files/files3.patch").build();

        offline.apply(patch);
        assertTrue(patched.exists());

        offline.rollback(patch);
        assertFalse(patched.exists());

        //just for fun, let's apply and rollback once more
        offline.apply(patch);
        assertTrue(patched.exists());

        offline.rollback(patch);
        assertFalse(patched.exists());
    }
}
