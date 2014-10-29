package io.fabric8.gateway.fabric.support.vertx;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.TestControl;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.vertx.java.core.Vertx;

@TestControl(startScopes = SessionScoped.class)
@RunWith(CdiTestRunner.class)
public class VertxServiceTest {

    
    @Inject VertxService vertxService;
    
    @Test
    public void VertxServiceExistsTest() {
        Assert.assertNotNull(vertxService);
        Assert.assertEquals(VertxServiceImpl.class.getName(), vertxService.getClass().getSuperclass().getName());
    }
    
    @Test
    public void VertxExistsTest() {
        Vertx vertx = vertxService.getVertx();
        Assert.assertNotNull(vertx);
        Assert.assertNotNull(vertxService.getCurator());
        System.out.println("test");
    }
}
