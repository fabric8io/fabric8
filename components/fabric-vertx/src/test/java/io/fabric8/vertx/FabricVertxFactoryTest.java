package io.fabric8.vertx;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import io.fabric8.vertx.internal.FabricVertxFactoryImpl;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class FabricVertxFactoryTest {
    
    @Inject
    FabricVertexFactory factory;
    
    @Test //making sure this resolves to our default implementation
    public void testFactoryInjection() {
        assertNotNull(factory);
        assertEquals(factory.getClass().getSuperclass().getName(), FabricVertxFactoryImpl.class.getName());
    }
    


}
