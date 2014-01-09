package io.fabric8.service.jclouds.internal;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.DynamicReference;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.service.jclouds.ComputeRegistry;
import org.fusesource.insight.log.support.Strings;
import org.jclouds.compute.ComputeService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;

@ThreadSafe
@Component(name = "io.fabric8.jclouds.compute.registry", label = "Fabric8 Jclouds Compute Registry", immediate = true, metatype = false)
@Service(ComputeRegistry.class)
public class ComputeRegistryImpl extends AbstractComponent implements ComputeRegistry {

    private static final Long COMPUTE_SERVICE_WAIT = 60000L;

    @Reference(cardinality = OPTIONAL_MULTIPLE, bind = "bindComputeService", unbind = "unbindComputeService", referenceInterface = ComputeService.class, policy = ReferencePolicy.DYNAMIC)
    @GuardedBy("ConcurrentHashMap") private final ConcurrentMap<String, DynamicReference<ComputeService>> computeServices = new ConcurrentHashMap<String, DynamicReference<ComputeService>>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public List<ComputeService> list() {
        assertValid();
        List<ComputeService> list = new ArrayList<ComputeService>();
        for (Map.Entry<String,DynamicReference<ComputeService>> entry : computeServices.entrySet()) {
            ComputeService computeService = entry.getValue().getIfPresent();
            if (computeService != null) {
                list.add(computeService);
            }
        }
        return list;
    }

    @Override
    public ComputeService getIfPresent(String name) {
        assertValid();
        computeServices.putIfAbsent(name, new DynamicReference<ComputeService>(name, COMPUTE_SERVICE_WAIT, TimeUnit.MILLISECONDS));
        return computeServices.get(name).getIfPresent();
    }

    /**
     * Finds or waits for the {@link org.jclouds.compute.ComputeService} that matches the specified name.
     */
    public ComputeService getOrWait(String name) {
        assertValid();
        computeServices.putIfAbsent(name, new DynamicReference<ComputeService>(name, COMPUTE_SERVICE_WAIT, TimeUnit.MILLISECONDS));
        return computeServices.get(name).get();
    }

    /**
     * Removes the {@link org.jclouds.compute.ComputeService} that matches the specified name.
     */
    public void remove(String name) {
        assertValid();
        DynamicReference<ComputeService> ref = computeServices.get(name);
        if (ref != null) {
            ref.unbind(null);
        }
    }

    void bindComputeService(ComputeService computeService) {
        String name = computeService.getContext().unwrap().getName();
        if (!Strings.isEmpty(name)) {
            computeServices.putIfAbsent(name, new DynamicReference<ComputeService>(name, COMPUTE_SERVICE_WAIT, TimeUnit.MILLISECONDS));
            computeServices.get(name).bind(computeService);
        }
    }

    void unbindComputeService(ComputeService computeService) {
        String name = computeService.getContext().unwrap().getName();
        if (!Strings.isEmpty(name)) {
            DynamicReference<ComputeService> ref = computeServices.get(name);
            if (ref != null) {
                ref.unbind(computeService);
            }
        }
    }
}
