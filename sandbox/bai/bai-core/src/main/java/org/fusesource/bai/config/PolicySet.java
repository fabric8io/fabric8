package org.fusesource.bai.config;

import org.apache.camel.management.event.AbstractExchangeEvent;
import org.apache.camel.util.ObjectHelper;
import org.fusesource.bai.AuditEvent;
import org.fusesource.bai.agent.CamelContextService;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

/**
 * Represents a set of auditing policies
 */
@XmlRootElement(name = "policySet")
@XmlAccessorType(XmlAccessType.FIELD)
public class PolicySet {
    @XmlElementRef
    private List<Policy> policies = new ArrayList<Policy>();

    public PolicySet() {
    }

    public PolicySet(List<Policy> policies) {
        this.policies = policies;
    }

    @Override
    public String toString() {
        return "PolicySet(" + policies + ")";
    }

    /**
     * Returns true if this policy set has at least one policy
     */
    public boolean hasPolicies() {
        if (policies != null) {
            for (Policy policy : policies) {
                if (policy.isEnabled()) return true;
            }
        }
        return false;
    }

    /**
     * Returns a configuration for this {@link CamelContextService} which policies out all of the non-applicable policies
     */
    public PolicySet createConfig(CamelContextService contextService) {
        List<Policy> matching = new ArrayList<Policy>();
        for (Policy policy : policies) {
            if (policy.matchesContext(contextService)) {
                matching.add(policy);
            }
        }
        return new PolicySet(matching);
    }

    /**
     * Returns true if the given event matches the policies
     */
    public boolean isEnabled(EventObject coreEvent, AbstractExchangeEvent exchangeEvent) {
        return hasPolicies();
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

    /**
     * Returns the policy for the given ID creating one on the fly if required
     */
    public Policy policy(String id) {
        for (Policy policy : policies) {
            if (ObjectHelper.equal(id, policy.getId())) {
                return policy;
            }
        }
        return addPolicy(new Policy(id));
    }

    private Policy addPolicy(Policy policy) {
        policies.add(policy);
        return policy;
    }

    // Properties
    //-------------------------------------------------------------------------
    public List<Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<Policy> policies) {
        this.policies = policies;
    }
}
