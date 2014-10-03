package io.fabric8.kubernetes.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;

import org.junit.Test;

public class KubernetesHelperTest {

	@Test
	public void testRemoveEmptyPods() throws Exception {
		
		PodSchema pod1 = new PodSchema();
		pod1.setId("test1");
		
		PodSchema pod2 = new PodSchema();
		
		PodListSchema podSchema = new PodListSchema();
		podSchema.getItems().add(pod1);
		podSchema.getItems().add(pod2);
		
		KubernetesHelper.removeEmptyPods(podSchema);
		
		assertNotNull(podSchema);
		assertEquals(1, podSchema.getItems().size());
	}
	
}
