/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 FuseSource Corporation, a Progress Software company. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").
 * You may not use this file except in compliance with the License. You can obtain
 * a copy of the License at http://www.opensource.org/licenses/CDDL-1.0.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at resources/META-INF/LICENSE.txt.
 *
 */
package org.fusesource.eca.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.camel.model.RouteDefinition;

/**
 * Represents an XML &lt;eca/&gt; element
 */
@XmlRootElement(name = "eca")
@XmlAccessorType(XmlAccessType.FIELD)
public class EcaRouteDefinition extends RouteDefinition {
    private String eventWindow = "30s";
    private final String name;
    private final EcaRoutesDefinition ecaRoutesDefinition;
    private final EcaDefinition ecaDefinition;

    public static EcaRouteDefinition transform(RouteDefinition original) {
        if (original instanceof EcaRouteDefinition) {
            return (EcaRouteDefinition) original;
        }
        EcaRouteDefinition result = new EcaRouteDefinition();
        result.setInputs(original.getInputs());
        result.setOutputs(original.getOutputs());
        result.setGroup(original.getGroup());
        result.setStreamCache(original.getStreamCache());
        result.setTrace(original.getTrace());
        result.setHandleFault(original.getHandleFault());
        result.setDelayer(original.getDelayer());
        result.setAutoStartup(original.getAutoStartup());
        result.setStartupOrder(original.getStartupOrder());
        result.setRoutePolicies(original.getRoutePolicies());
        result.setShutdownRoute(original.getShutdownRoute());
        result.setShutdownRunningTask(original.getShutdownRunningTask());

        return result;
    }

    public EcaRouteDefinition() {
        this.name = null;
        this.ecaRoutesDefinition = null;
        this.ecaDefinition = null;
    }

    public EcaRouteDefinition(String name, EcaRoutesDefinition espRoutesDefinition) {
        this.name = name;
        this.ecaRoutesDefinition = espRoutesDefinition;
        this.ecaDefinition = new EcaDefinition(this.name);
        getInputs().add(this.ecaDefinition);
    }

    public EcaDefinition getEcaDefinition() {
        return this.ecaDefinition;
    }

    @Override
    public String toString() {
        String str = name != null ? "eca(" + name + ")" : "route";
        str += "[" + getInputs() + "-> ";
        if (ecaDefinition != null && ecaDefinition.getEventPatterns() != null && ecaDefinition.getEventPatterns().isEmpty() == false) {
            str += "[" + ecaDefinition.getEventPatterns() + "]";
        }
        str += " ->" + getOutputs() + "]";
        return str;
    }

    @Override
    public String getShortName() {
        return "route";
    }

    public EcaRouteDefinition window(String str) {
        setEventWindow(str);
        ecaDefinition.setEventWindow(str);
        return this;
    }

    public EcaRouteDefinition win(String str) {
        setEventWindow(str);
        ecaDefinition.setEventWindow(str);
        return this;
    }

    public String getEventWindow() {
        return eventWindow;
    }

    public void setEventWindow(String eventWindow) {
        this.eventWindow = eventWindow != null ? eventWindow.trim() : null;
    }

    public EcaRouteDefinition when(String targetId) {
        RouteDefinition routeDefinition = this.ecaRoutesDefinition.getRouteDefinition(targetId);
        if (routeDefinition != null) {
            EcaEventPattern ecaEventPattern = new EcaEventPattern(routeDefinition, EcaEventPattern.TYPE.WHEN);
            ecaDefinition.addEventPattern(ecaEventPattern);
        } else {
            IllegalArgumentException cause = new IllegalArgumentException("Route " + targetId + " cannot be found");
            throw cause;
        }
        return this;
    }

    public EcaRouteDefinition when(RouteDefinition routeDefinition) {
        if (routeDefinition != null) {
            EcaEventPattern ecaEventPattern = new EcaEventPattern(routeDefinition, EcaEventPattern.TYPE.WHEN);
            ecaDefinition.addEventPattern(ecaEventPattern);
        } else {
            IllegalArgumentException cause = new IllegalArgumentException("Route is Null");
            throw cause;
        }
        return this;
    }

    public EcaRouteDefinition and(String targetId) {
        RouteDefinition routeDefinition = this.ecaRoutesDefinition.getRouteDefinition(targetId);
        if (routeDefinition != null) {
            EcaEventPattern ecaEventPattern = new EcaEventPattern(routeDefinition, EcaEventPattern.TYPE.AND);
            ecaDefinition.addEventPattern(ecaEventPattern);
        } else {
            IllegalArgumentException cause = new IllegalArgumentException("Route " + targetId + " cannot be found");
            throw cause;
        }
        return this;
    }

    public EcaRouteDefinition and(RouteDefinition routeDefinition) {
        if (routeDefinition != null) {
            EcaEventPattern ecaEventPattern = new EcaEventPattern(routeDefinition, EcaEventPattern.TYPE.AND);
            ecaDefinition.addEventPattern(ecaEventPattern);
        } else {
            IllegalArgumentException cause = new IllegalArgumentException("Route is Null");
            throw cause;
        }
        return this;
    }

    public EcaRouteDefinition or(String targetId) {
        RouteDefinition routeDefinition = this.ecaRoutesDefinition.getRouteDefinition(targetId);
        if (routeDefinition != null) {
            EcaEventPattern ecaEventPattern = new EcaEventPattern(routeDefinition, EcaEventPattern.TYPE.OR);
            ecaDefinition.addEventPattern(ecaEventPattern);
        } else {
            IllegalArgumentException cause = new IllegalArgumentException("Route " + targetId + " cannot be found");
            throw cause;
        }
        return this;
    }

    public EcaRouteDefinition or(RouteDefinition routeDefinition) {
        if (routeDefinition != null) {
            EcaEventPattern ecaEventPattern = new EcaEventPattern(routeDefinition, EcaEventPattern.TYPE.OR);
            ecaDefinition.addEventPattern(ecaEventPattern);
        } else {
            IllegalArgumentException cause = new IllegalArgumentException("Route is Null");
            throw cause;
        }
        return this;
    }

    public EcaRouteDefinition not(String targetId) {
        RouteDefinition routeDefinition = this.ecaRoutesDefinition.getRouteDefinition(targetId);
        if (routeDefinition != null) {
            EcaEventPattern ecaEventPattern = new EcaEventPattern(routeDefinition, EcaEventPattern.TYPE.NOT);
            ecaDefinition.addEventPattern(ecaEventPattern);
        } else {
            IllegalArgumentException cause = new IllegalArgumentException("Route " + targetId + " cannot be found");
            throw cause;
        }
        return this;
    }

    public EcaRouteDefinition not(RouteDefinition routeDefinition) {
        if (routeDefinition != null) {
            EcaEventPattern ecaEventPattern = new EcaEventPattern(routeDefinition, EcaEventPattern.TYPE.NOT);
            ecaDefinition.addEventPattern(ecaEventPattern);
        } else {
            IllegalArgumentException cause = new IllegalArgumentException("Route is Null");
            throw cause;
        }
        return this;
    }

    public EcaRouteDefinition before(String targetId) {
        RouteDefinition routeDefinition = this.ecaRoutesDefinition.getRouteDefinition(targetId);
        if (routeDefinition != null) {
            EcaEventPattern ecaEventPattern = new EcaEventPattern(routeDefinition, EcaEventPattern.TYPE.BEFORE);
            ecaDefinition.addEventPattern(ecaEventPattern);
        } else {
            IllegalArgumentException cause = new IllegalArgumentException("Route " + targetId + " cannot be found");
            throw cause;
        }
        return this;
    }

    public EcaRouteDefinition before(RouteDefinition routeDefinition) {
        if (routeDefinition != null) {
            EcaEventPattern ecaEventPattern = new EcaEventPattern(routeDefinition, EcaEventPattern.TYPE.BEFORE);
            ecaDefinition.addEventPattern(ecaEventPattern);
        } else {
            IllegalArgumentException cause = new IllegalArgumentException("Route is Null");
            throw cause;
        }
        return this;
    }

    public EcaRouteDefinition after(String targetId) {
        RouteDefinition routeDefinition = this.ecaRoutesDefinition.getRouteDefinition(targetId);
        if (routeDefinition != null) {
            EcaEventPattern ecaEventPattern = new EcaEventPattern(routeDefinition, EcaEventPattern.TYPE.AFTER);
            ecaDefinition.addEventPattern(ecaEventPattern);
        } else {
            IllegalArgumentException cause = new IllegalArgumentException("Route " + targetId + " cannot be found");
            throw cause;
        }
        return this;
    }

    public EcaRouteDefinition after(RouteDefinition routeDefinition) {
        if (routeDefinition != null) {
            EcaEventPattern ecaEventPattern = new EcaEventPattern(routeDefinition, EcaEventPattern.TYPE.AFTER);
            ecaDefinition.addEventPattern(ecaEventPattern);
        } else {
            IllegalArgumentException cause = new IllegalArgumentException("Route is Null");
            throw cause;
        }
        return this;
    }

    public EcaRouteDefinition evaluate(String pattern) {
        ecaDefinition.setPattern(pattern);
        return this;
    }
}
