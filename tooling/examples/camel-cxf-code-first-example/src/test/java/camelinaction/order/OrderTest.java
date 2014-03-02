package camelinaction.order;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.BeforeClass;
import org.junit.Test;

public class OrderTest extends CamelBlueprintTestSupport {

    @BeforeClass
    public static void setupPort() {
        int port = AvailablePortFinder.getNextAvailable(10000);
        System.setProperty("port", "" + port);
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "camel-route-test.xml";
    }

    @Test
    public void testOrderOk() throws Exception {
        List<Object> params = new ArrayList<Object>();
        params.add("motor");
        params.add(1);
        params.add("honda");
        
        String reply = template.requestBody("cxf:bean:orderEndpoint", params, String.class);
        assertEquals("OK", reply);
    }
}
