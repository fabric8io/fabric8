package io.fabric8.zookeeper.utils;

import java.net.URL;

import org.junit.Test;

public class ZookeeperImportUtilsTest {

	@Test
	public void testNoExceptionFromImportProperties () throws Exception {
		URL url = this.getClass().getResource("/import-test.properties");
		ZookeeperImportUtils.importFromPropertiesFile(null, url.toString(), "mypid", null, null, true);
	}
}
