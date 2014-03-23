package io.fabric8.insight.camel.base;

import org.apache.camel.CamelContext;
import org.apache.camel.Service;
import org.apache.camel.management.DefaultManagementNamingStrategy;
import org.apache.camel.util.ObjectHelper;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 *
 */
public class InsightManagementNamingStrategy extends DefaultManagementNamingStrategy {

    public static final String TYPE_FABRIC = "fabric";
    public static final String TYPE_INSIGHT = "insight";

    public InsightManagementNamingStrategy(String domainName) {
        super(domainName);
    }

    @Override
    public ObjectName getObjectNameForService(CamelContext context, Service service) throws MalformedObjectNameException {
        StringBuilder buffer = new StringBuilder();
        buffer.append(domainName).append(":");
        buffer.append(KEY_CONTEXT + "=").append(getContextId(context)).append(",");
        // special for fabric
        if (service.getClass().getCanonicalName().startsWith("org.apache.camel.fabric")) {
            buffer.append(KEY_TYPE + "=" + TYPE_FABRIC + ",");
        } else if (service.getClass().getCanonicalName().startsWith("org.fusesource.insight.camel")) {
            buffer.append(KEY_TYPE + "=" + TYPE_INSIGHT + ",");
        } else {
            buffer.append(KEY_TYPE + "=" + TYPE_SERVICE + ",");
        }
        buffer.append(KEY_NAME + "=")
                .append(service.getClass().getSimpleName())
                .append("(").append(ObjectHelper.getIdentityHashCode(service)).append(")");
        return createObjectName(buffer);
    }

}
