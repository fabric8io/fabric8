/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 */
package io.fabric8.stream.log;

import org.apache.activemq.util.IntrospectionSupport;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * <p>
 * Implements a OGSi ManagedServiceFactory so that we can
 * easily configure HttpSimulators easily across a fabric
 * cluster.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class HttpSimulatorFactory implements ManagedServiceFactory {

    HashMap<String, HttpSimulator> simulators = new HashMap<String, HttpSimulator>();

    @Override
    public String getName() {
        return "Http Simulator Factory";
    }


    @Override
    synchronized public void updated(String pid, Dictionary dictionary) throws ConfigurationException {
        try {
            deleted(pid);
            simulators.put(pid, createSimulator(toMap(dictionary)));
        } catch (Throwable e) {
            throw (ConfigurationException) new ConfigurationException(null, "Unable to parse configuration: " + e.getMessage()).initCause(e);
        }
    }

    @Override
    synchronized public void deleted(String s) {
        HttpSimulator simulator = simulators.remove(s);
        if (simulator != null) {
            simulator.stop();
        }
    }

    public void destroy() {
        for( String key : new ArrayList<String>(simulators.keySet()) ) {
            deleted(key);
        }
    }

    private HttpSimulator createSimulator(HashMap<String, String> properties) {
        HttpSimulator simulator = new HttpSimulator();
        IntrospectionSupport.setProperties(simulator, properties);
        simulator.start();
        return simulator;
    }

    public static HashMap<String, String> toMap(Dictionary dictionary) {
        HashMap<String, String> rc = new HashMap<String, String>();
        Enumeration ek = dictionary.keys();
        while (ek.hasMoreElements()) {
            Object key = ek.nextElement();
            Object value = dictionary.get(key);
            if(key!=null && value!=null) {
                rc.put(key.toString(), value.toString());
            }
        }
        return rc;
    }

}
