package io.fabric8.gateway.fabric.support.http;

import static io.fabric8.kubernetes.api.KubernetesHelper.getId;
import static io.fabric8.kubernetes.api.KubernetesHelper.getPort;
import static io.fabric8.kubernetes.api.KubernetesHelper.getSelector;
import static java.util.concurrent.TimeUnit.SECONDS;
import io.fabric8.gateway.ServiceDTO;
import io.fabric8.gateway.api.handlers.http.HttpMappingRule;
import io.fabric8.gateway.fabric.http.HTTPGatewayConfig;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.fabric8.kubernetes.api.model.ServiceSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpMappingKubeCache implements Runnable {

    private static final transient Logger LOG = LoggerFactory.getLogger(HttpMappingKubeCache.class);

    private final ScheduledExecutorService serviceCacheExecutor = Executors.newSingleThreadScheduledExecutor();
    private KubernetesClient client;
    private final HttpMappingRule mappingRuleConfiguration;
    private final List<Map<String,String>> serviceSelectors;
    private List<String> contextPathsCache;
    
    public HttpMappingKubeCache(HttpMappingRule mappingRuleConfiguration, List<Map<String,String>> serviceSelectors) {
       this.mappingRuleConfiguration = mappingRuleConfiguration;
       this.serviceSelectors = serviceSelectors;
    }
    
    public void init(HTTPGatewayConfig configuation) {
        String kubernetesMaster = configuation.getKubernetesMaster();
        KubernetesFactory factory = new KubernetesFactory(kubernetesMaster);
        contextPathsCache = new ArrayList<String>();
        client = new KubernetesClient(factory);
        //for now simply check in with kubernetes every 5 seconds
        //it'd be nice if kubernetes can callback into our cache.
        serviceCacheExecutor.scheduleWithFixedDelay(this, 0, 5, SECONDS);
    }
    
    public void destroy() {
        serviceCacheExecutor.shutdown();
    }
    
    protected static String paramValue(String paramValue) {
        return paramValue != null ? paramValue : "";
    }

    /**
     * All fields in the Gateway Selector need to match with the serviceSelector argument
     * @param selector
     * @return true if all gateway selector fields are matched
     */
    private boolean selectorMatch(Map<String,String> selector) {
    	
    	for (Map<String,String> serviceSelector : serviceSelectors) {
    		boolean isMatch = true;
	        for (String key : serviceSelector.keySet()) {
	            if ( !selector.containsKey(key) || !selector.get(key).equals(serviceSelector.get(key)) ){
	            	isMatch = false;
	            	break;
	            }
	        }
	        if (isMatch) return true;
    	}
        return false;
    }

    @Override
    public void run() {
        this.refreshServices();
    }
    
    public void refreshServices() {
        List<String> currentCache = new ArrayList<String>();
        currentCache.addAll(contextPathsCache);
        try {
            ServiceList serviceList = client.getServices();
            for (Service service1 : serviceList.getItems()) {
                Map<String, String> selector = getSelector(service1);
                if (selectorMatch(selector)) {
                    String contextPath = getId(service1);
                    
                    ServiceDTO dto = new ServiceDTO();
                    dto.setId(getId(service1));
                    dto.setContainer(selector.get("container"));
                    dto.setVersion(selector.get("version"));
                    
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("id", paramValue(dto.getId()));
                    params.put("container", paramValue(dto.getContainer()));
                    params.put("version", paramValue(dto.getVersion()));
                    //is there a better way to obtain the complete url?
                    String service = "http://localhost:" + getPort(service1) + "/" + getId(service1);
                    List<String> services = Arrays.asList(service);
                    if (!contextPathsCache.contains(contextPath)) {
                        LOG.info("Adding " + service);
                        contextPathsCache.add(contextPath);
                    }
                    mappingRuleConfiguration.updateMappingRules(false, contextPath,
                            services, params, dto);
                    currentCache.remove(contextPath);
                }
            }
            //Removing services that we still have in our cache, but are no longer in kubernetes
            for (String contextPath: currentCache) {
                mappingRuleConfiguration.updateMappingRules(true, contextPath,
                        null, null, null);
                LOG.info("Removing " + contextPath);
                contextPathsCache.remove(contextPath);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
