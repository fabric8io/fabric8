package io.fabric8.karaf.checks.internal;

import io.fabric8.karaf.checks.Check;
import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.osgi.framework.Bundle;
import org.osgi.util.tracker.ServiceTracker;

public class ScrState extends AbstractBundleChecker {

    private final ServiceTracker<ScrService, ScrService> tracker;

    public ScrState() {
        super();
        tracker = new ServiceTracker<>(bundleContext, ScrService.class, null);
        tracker.open();
    }

    @Override
    protected Check checkBundle(Bundle bundle) {
        if (bundle.getHeaders().get("Service-Component") == null) {
            return null;
        }
        ScrService svc = tracker.getService();
        if (svc == null) {
            return new Check("scr-state", "No ScrService found");
        }
        Component[] components = svc.getComponents(bundle);
        if (components != null) {
            for (Component component : components) {
                int state = component.getState();
                if (state != Component.STATE_ACTIVE && state != Component.STATE_REGISTERED
                        && state != Component.STATE_FACTORY) {
                    return new Check("scr-state", "SCR bundle " + bundle.getBundleId() + " is in state " + getState(state));
                }
            }
        }
        return null;
    }

    private String getState(int state) {
        switch (state) {
            case (Component.STATE_DISABLED):
                return "disabled";
            case (Component.STATE_ENABLING):
                return "enabling";
            case (Component.STATE_ENABLED):
                return "enabled";
            case (Component.STATE_UNSATISFIED):
                return "unsatisfied";
            case (Component.STATE_ACTIVATING):
                return "activating";
            case (Component.STATE_ACTIVE):
                return "active";
            case (Component.STATE_REGISTERED):
                return "registered";
            case (Component.STATE_FACTORY):
                return "factory";
            case (Component.STATE_DEACTIVATING):
                return "deactivating";
            case (Component.STATE_DISABLING):
                return "disabling";
            case (Component.STATE_DISPOSING):
                return "disposing";
            case (Component.STATE_DISPOSED):
                return "disposed";
            default:
                return "unknown: " + state;
        }
    }

}
