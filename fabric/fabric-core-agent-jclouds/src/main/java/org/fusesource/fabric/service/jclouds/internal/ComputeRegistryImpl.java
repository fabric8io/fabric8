package org.fusesource.fabric.service.jclouds.internal;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.DynamicReference;
import org.fusesource.fabric.service.jclouds.ComputeRegistry;
import org.fusesource.insight.log.support.Strings;
import org.jclouds.compute.ComputeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;

@Component(name = "org.fusesource.fabric.jclouds.compute.registry",
        description = "Fabric Jclouds Compute Registry",
        immediate = true)
@Service(ComputeRegistry.class)
public class ComputeRegistryImpl implements ComputeRegistry {

    private static final Long COMPUTE_SERVICE_WAIT = 60000L;

    @Reference(cardinality = OPTIONAL_MULTIPLE, bind = "bindComputeService", unbind = "unbindComputeService", referenceInterface = ComputeService.class, policy = ReferencePolicy.DYNAMIC)
    private final ConcurrentMap<String, DynamicReference<ComputeService>> map = new ConcurrentHashMap<String, DynamicReference<ComputeService>>();

    @Override
    public List<ComputeService> list() {
        List<ComputeService> list = new ArrayList<ComputeService>();
        for (Map.Entry<String,DynamicReference<ComputeService>> entry : map.entrySet()) {
            ComputeService computeService = entry.getValue().getIfPresent();
            if (computeService != null) {
                list.add(computeService);
            }
        }
        return list;
    }

    @Override
    public ComputeService getIfPresent(String name) {
        map.putIfAbsent(name, new DynamicReference<ComputeService>(name, COMPUTE_SERVICE_WAIT, TimeUnit.MILLISECONDS));
        return map.get(name).getIfPresent();
    }

    /**
     * Finds or waits for the {@link org.jclouds.compute.ComputeService} that matches the specified name.
     * @param name
     * @return
     */
    public ComputeService getOrWait(String name) {
        map.putIfAbsent(name, new DynamicReference<ComputeService>(name, COMPUTE_SERVICE_WAIT, TimeUnit.MILLISECONDS));
        return map.get(name).get();
    }

    /**
     * Removes the {@link org.jclouds.compute.ComputeService} that matches the specified name.
     * @param name
     * @return
     */
    public void remove(String name) {
        DynamicReference<ComputeService> ref = map.get(name);
        if (ref != null) {
            ref.unbind();
        }
    }



    public void bindComputeService(ComputeService computeService) {
        String name = computeService.getContext().unwrap().getName();
        if (!Strings.isEmpty(name)) {
            map.putIfAbsent(name, new DynamicReference<ComputeService>(name, COMPUTE_SERVICE_WAIT, TimeUnit.MILLISECONDS));
            map.get(name).bind(computeService);
        }
    }

    public void unbindComputeService(ComputeService computeService) {
        String name = computeService.getContext().unwrap().getName();
        if (!Strings.isEmpty(name)) {
            DynamicReference<ComputeService> ref = map.get(name);
            if (ref != null) {
                ref.unbind();
            }
        }
    }
}
