package org.fusesource.bai.config;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.camel.management.event.AbstractExchangeEvent;
import org.fusesource.bai.AuditEvent;
import org.fusesource.bai.agent.CamelContextService;

/**
 * Represents a set of auditing policies
 */
@XmlRootElement(name = "policySet")
@XmlAccessorType(XmlAccessType.FIELD)
public class PolicySet {
    @XmlAttribute()
    private String endpointUri = "vm:audit";
    @XmlElementRef
    private List<Policy> policies = new ArrayList<Policy>();

    public PolicySet() {
    }

    public PolicySet(String endpointUri, List<Policy> policies) {
        this.endpointUri = endpointUri;
        this.policies = policies;
    }

    @Override
    public String toString() {
        return "PolicySet(endpoint: " + endpointUri + ", policies: " + policies + ")";
    }

    /**
     * Returns a configuration for this {@link CamelContextService} which policies out all of the non-applicable policies
     */
    public PolicySet createConfig(CamelContextService contextService) {
        List<Policy> matching = new ArrayList<Policy>();
        for (Policy policy : policies) {
            if (policy.isEnabled() && policy.matchesContext(contextService)) {
                matching.add(policy);
            }
        }
        if (matching.isEmpty()) {
            return null;
        } else {
            return new PolicySet(endpointUri, matching);
        }
    }

    /**
     * Returns true if the given event matches the policies
     */
    public boolean isEnabled(EventObject coreEvent, AbstractExchangeEvent exchangeEvent) {
        // TODO
        return true;
    }

    public boolean matchesEvent(AuditEvent auditEvent) {
        for (Policy policy : policies) {
            if (policy.matchesEvent(auditEvent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the payload
     */
    public Object createPayload(AuditEvent auditEvent) {
        return auditEvent;
    }


    public Policy addPolicy(String id) {
        return addPolicy(new Policy(id));
    }

    private Policy addPolicy(Policy policy) {
        policies.add(policy);
        return policy;
    }

    // Properties
    //-------------------------------------------------------------------------
    public String getEndpointUri() {
        return endpointUri;
    }

    public void setEndpointUri(String endpointUri) {
        this.endpointUri = endpointUri;
    }

    public List<Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<Policy> policies) {
        this.policies = policies;
    }

}
