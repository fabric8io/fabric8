package org.fusesource.fabric.agent.repository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.osgi.resource.Resource;

public class Maven2RepositoryTest {

    @Test
    public void testMaven2() throws Exception {

        String home = System.getProperty("user.home");
        final Path root = Paths.get(home, ".m2/repository");
        final List<String> groupIds = Arrays.asList("org.apache");

        long t0 = System.currentTimeMillis();
        Maven2MetadataProvider mp = new Maven2MetadataProvider(root, groupIds);
        MetadataRepository repo = new MetadataRepository(mp);
        long t1 = System.currentTimeMillis();

        for (Resource resource : repo.getResources()) {
            System.out.println(resource);
        }

        System.out.println("Took " + (t1 - t0) + " ms to index " + repo.getResources().size() + " resources");


//        WatchService watch = FileSystems.getDefault().newWatchService();
//        watch.

    }
}
