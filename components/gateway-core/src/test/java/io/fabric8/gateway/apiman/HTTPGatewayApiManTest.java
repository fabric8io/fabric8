/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.gateway.apiman;


import static org.junit.Assert.assertEquals;
import io.fabric8.gateway.api.CallDetailRecord;
import io.fabric8.gateway.api.apimanager.ApiManager;
import io.fabric8.gateway.api.apimanager.ApiManagerService;
import io.fabric8.gateway.api.handlers.http.HttpGateway;
import io.fabric8.gateway.api.handlers.http.HttpGatewayHandler;
import io.fabric8.gateway.api.handlers.http.HttpMappingRule;
import io.fabric8.gateway.api.handlers.http.IMappedServices;
import io.fabric8.gateway.apiman.ApiManService;
import io.fabric8.gateway.apiman.FileBackedRegistry;
import io.fabric8.gateway.ServiceDTO;
import io.fabric8.gateway.ServiceMap;
import io.fabric8.gateway.handlers.detecting.DetectingGatewayWebSocketHandler;
import io.fabric8.gateway.handlers.detecting.FutureHandler;
import io.fabric8.gateway.handlers.http.HttpGatewayServer;
import io.fabric8.gateway.handlers.http.MappedServices;
import io.fabric8.gateway.loadbalancer.LoadBalancer;
import io.fabric8.gateway.loadbalancer.RoundRobinLoadBalancer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.overlord.apiman.engine.policies.IPBlacklistPolicy;
import org.overlord.apiman.rt.engine.beans.Application;
import org.overlord.apiman.rt.engine.beans.Contract;
import org.overlord.apiman.rt.engine.beans.Policy;
import org.overlord.apiman.rt.engine.beans.Service;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 */
public class HTTPGatewayApiManTest {

	final static HashMap<String, IMappedServices> mappedServices = new HashMap<String, IMappedServices>();
    ServiceMap serviceMap = new ServiceMap();

    // Setup Vertx
    protected static Vertx vertx;
    protected static HttpServer restApplication;
    protected static HttpGatewayServer httpGatewayServer;
    
    private static String silverHelloServiceApiKey = "silver-key";
    private static String goldHelloServiceApiKey = "gold-key";
    
    @BeforeClass
    public static void startVertx() throws InterruptedException, IOException {
    	//deleting the registry
    	FileBackedRegistry.getRegistryFile().delete();
    	
        if( vertx == null ) {
            vertx = VertxFactory.newVertx();
        }
        startRestApplication();
        startHttpGateway();
    }
    @AfterClass
    public static void stopVertx() throws IOException, InterruptedException{
    	stopHttpGateway();
        endRestEndpoint();
        if( vertx!=null ) {
            vertx.stop();
            vertx = null;
        }
    }

    /*
     * This REST endpoint server simulates a user application. It replies with "Hello World!"
     * for a GET request using any path on http://localhost:18181.
     */
    public static HttpServer startRestApplication() throws InterruptedException, IOException {
    	
        restApplication = vertx.createHttpServer();
        restApplication.requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest request) {
                request.response().putHeader("content-type", "text/plain");
                request.response().end("Hello World!");
            }
        });

        FutureHandler<AsyncResult<HttpServer>> future = new FutureHandler<>();
        restApplication.listen(18181, "localhost", future);
        future.await();
        System.out.println("Rest Application is up and listening at http://localhost:18181");
        return restApplication;
    }

    public static void endRestEndpoint() throws InterruptedException {
        if( restApplication !=null ) {
            restApplication.close();
            restApplication = null;
        }
    }

    /*
     * Starts the HTTPGateway - listening at port 18080.
     */
    public static HttpGatewayServer startHttpGateway() throws HttpException, IOException {

    	System.out.println("Starting HttpGateway with a mapping of /hello/world to the Rest Application");
        if( restApplication!=null ) {
            LoadBalancer loadBalancer=new RoundRobinLoadBalancer();

            ServiceDTO serviceDetails = new ServiceDTO();
            serviceDetails.setContainer("local");
            serviceDetails.setVersion("1");

            mappedServices.put("/hello/world", new MappedServices("http://localhost:18181", serviceDetails, loadBalancer, false));
        
        }
        
        final ApiManager apiManager = new ApiManager();
        
        DetectingGatewayWebSocketHandler websocketHandler = new DetectingGatewayWebSocketHandler();
        final HttpGateway httpGateway = new HttpGateway(){
        	
        	@Override
            public void addMappingRuleConfiguration(HttpMappingRule mappingRule) {
            }

            @Override
            public void removeMappingRuleConfiguration(HttpMappingRule mappingRule) {
            }

            @Override
            public Map<String, IMappedServices> getMappedServices() {
                return mappedServices;
            }

            @Override
            public boolean isEnableIndex() {
                return true;
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return new InetSocketAddress("0.0.0.0", 18080);
            }

			@Override
			public void addCallDetailRecord(CallDetailRecord cdr) {
			}

			@Override
			public ApiManager getApiManager() {
				if (apiManager.getService() == null) {
					ApiManagerService apiManagerService = new ApiManService();
					Map<String, Object> config = new HashMap<String,Object>();
			        config.put("vertx", vertx);
			        config.put("httpGateway", this);
			        config.put("port", String.valueOf(this.getLocalAddress().getPort()));
					apiManagerService.init(config);
					apiManager.setService(apiManagerService);
				}
				return apiManager;
			}

        };
        
        websocketHandler.setPathPrefix("");
        
        Handler<HttpServerRequest> requestHandler = null;
        if (httpGateway.getApiManager().isApiManagerEnabled()) {
        	requestHandler = apiManager.getService().createApiManagerHttpGatewayHandler();
		} else {
			requestHandler = new HttpGatewayHandler(vertx, httpGateway);
		}
        
        httpGatewayServer = new HttpGatewayServer(vertx, websocketHandler, 18080, requestHandler);
        httpGatewayServer.setHost("0.0.0.0");
        httpGatewayServer.init();
        System.out.println("HttpGateway started");
        
        configureEngine();
      
        return httpGatewayServer;
    }

    public static void stopHttpGateway() throws IOException{
        if( httpGatewayServer!=null ) {
        	cleanAddedConfig();
            httpGatewayServer.destroy();
            httpGatewayServer = null;
        }
    }

    @Test
    public void testGoldClientRequest() throws Exception {

        int httpPort = httpGatewayServer.getPort();
        HttpClient httpClient = new HttpClient();
       
        HttpMethod method = new GetMethod("http://127.0.0.1:" + httpPort + "/hello/world?apikey=gold-key");
        assertEquals(200, httpClient.executeMethod(method));
        String content = method.getResponseBodyAsString();
        assertEquals("Hello World!",content);

    }
    
    @Test
    public void testSilverClientRequest() throws Exception {
    	
        int httpPort = httpGatewayServer.getPort();
        HttpClient httpClient = new HttpClient();
        HttpMethod method = new GetMethod("http://127.0.0.1:" + httpPort + "/hello/world?apikey=silver-key");
        assertEquals(500, httpClient.executeMethod(method));
    }
    
    @Test
    public void testGoldClientJSONRequest() throws Exception {
        /** Tests obtaining the mapping info as JSON */
        int httpPort = httpGatewayServer.getPort();
        HttpClient httpClient = new HttpClient();
        HttpMethod method = new GetMethod("http://127.0.0.1:" + httpPort + "/?apikey=gold-key");
        assertEquals(200, httpClient.executeMethod(method));
        String content = method.getResponseBodyAsString();
        assertEquals("{\"/hello/world\":[\"http://localhost:18181\"]}",content);
    }
    
    @Test
    public void testGoldClientBadPathRequest() throws Exception {
        /** Tests obtaining the mapping info as JSON */
        int httpPort = httpGatewayServer.getPort();
        HttpClient httpClient = new HttpClient();
        HttpMethod method = new GetMethod("http://127.0.0.1:" + httpPort + "/mapping/notexist?apikey=gold-key");
        assertEquals(404, httpClient.executeMethod(method));
        String message = method.getStatusText();
        assertEquals("Could not find matching proxy path for /mapping/notexist?apikey=gold-key from paths: [/hello/world]",message);
    }

    public static void configureEngine() throws HttpException, IOException {
    	
    	int httpPort = httpGatewayServer.getPort();
        HttpClient httpClient = new HttpClient();
        
    	ObjectMapper mapper = new ObjectMapper();
    	
    	Service service = new Service();
        service.setServiceId("HelloWorld");
        service.setEndpoint("http://localhost:18181/");
        service.setVersion("1.0");
        service.setOrganizationId("Kurt");
        
        String serviceJson = mapper.writeValueAsString(service);
        
        System.out.println("Publishing HelloWorld Service");
        PutMethod method = new PutMethod("http://127.0.0.1:" + httpPort + "/rest/apimanager/api/services/?apikey=apiman-config-key");
        RequestEntity requestEntity = new StringRequestEntity(serviceJson, "application/json", "UTF-8");
        method.setRequestEntity(requestEntity);
        httpClient.executeMethod(method);
        
        
        Application clientApp = new Application();
        clientApp.setApplicationId("clientApp");
        clientApp.setOrganizationId("ClientOrg");
        clientApp.setVersion("1.0");
        
        Policy blackListPolicy = new Policy();
        blackListPolicy.setPolicyJsonConfig("{ \"ipList\" : [ \"127.0.0.1\" ] }");
        blackListPolicy.setPolicyImpl("class:" + IPBlacklistPolicy.class.getName());
        
        Contract silverContract = new Contract();
        silverContract.setApiKey(silverHelloServiceApiKey);
        silverContract.setServiceId(service.getServiceId());
        silverContract.setServiceOrgId(service.getOrganizationId());
        silverContract.setServiceVersion(service.getVersion());
        silverContract.getPolicies().add(blackListPolicy);
        clientApp.addContract(silverContract);
        
        Contract goldContract = new Contract();
        goldContract.setApiKey(goldHelloServiceApiKey);
        goldContract.setServiceId(service.getServiceId());
        goldContract.setServiceOrgId(service.getOrganizationId());
        goldContract.setServiceVersion(service.getVersion());
        clientApp.addContract(goldContract);
        
        String clientAppJson = mapper.writeValueAsString(clientApp);
        
        System.out.println("Register clientApp Application");
        method = new PutMethod("http://127.0.0.1:" + httpPort + "/rest/apimanager/api/applications/?apikey=apiman-config-key");
        requestEntity = new StringRequestEntity(clientAppJson, "application/json", "UTF-8");
        method.setRequestEntity(requestEntity);
        httpClient.executeMethod(method);
        
    }
    
    public static void cleanAddedConfig() throws IOException {
    	
    	int httpPort = httpGatewayServer.getPort();
        HttpClient httpClient = new HttpClient();
    	//Removing applications
    	System.out.println("Unregister clientApp Application");
        DeleteMethod method = new DeleteMethod("http://127.0.0.1:" + httpPort + "/rest/apimanager/api/applications/?apikey=apiman-config-key&organizationId=ClientOrg&applicationId=clientApp&version=1.0");
        httpClient.executeMethod(method);
        
        //Removing services
        System.out.println("Retire Hello World Service");
        method = new DeleteMethod("http://127.0.0.1:" + httpPort + "/rest/apimanager/api/services/?apikey=apiman-config-key&organizationId=Kurt&serviceId=HelloWorld&version=1.0");
        httpClient.executeMethod(method);
    }

}
