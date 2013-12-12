/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fabric8.dosgi.util;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A unique ID generator which is a fast implementation based on
 * how <a href="http://activemq.apache.org/>Apache ActiveMQ</a> generates its UUID.
 * <p/>
 */
public class UuidGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(UuidGenerator.class);
    private static final String UNIQUE_STUB;
    private static int instanceCount;
    private static String hostName;
    private String seed;
    private AtomicLong sequence = new AtomicLong(1);
    private int length;
    
    private static UuidGenerator instance = null;
    static {
        String stub = "";
        boolean canAccessSystemProps = true;
        try {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPropertiesAccess();
            }
        } catch (SecurityException se) {
            canAccessSystemProps = false;
        }

        if (canAccessSystemProps) {
            try {
                hostName = getLocalHostName();
                ServerSocket ss = new ServerSocket(0);
                stub = "-" + ss.getLocalPort() + "-" + System.currentTimeMillis() + "-";
                Thread.sleep(100);
                ss.close();
            } catch (Exception ioe) {
                LOG.warn("could not generate unique stub", ioe);
            }
        } else {
            hostName = "localhost";
            stub = "-1-" + System.currentTimeMillis() + "-";
        }
        UNIQUE_STUB = stub;
    }

    /**
     * Construct an IdGenerator
     */
    private UuidGenerator(String prefix) {
        synchronized (UNIQUE_STUB) {
        	int hashValue = prefix.hashCode();
        	if (hashValue < 0) {
        		hashValue = - hashValue;
        	}
            this.seed = hashValue + UNIQUE_STUB + (instanceCount++) + ":";
            this.seed = generateSanitizedId(this.seed);
            this.length = this.seed.length() + ("" + Long.MAX_VALUE).length();
        }
    }

    private UuidGenerator() {
        this("ID:" + hostName);
    }

    /**
     * As we have to find the hostname as a side-affect of generating a unique
     * stub, we allow it's easy retrevial here
     *
     * @return the local host name
     */

    public static String getHostName() {
        return hostName;
    }


    /**
     * Generate a unqiue id
     *
     * @return a unique id
     */

    private String generateId() {
        StringBuilder sb = new StringBuilder(length);
        sb.append(seed);
        sb.append(sequence.getAndIncrement());
        return sb.toString();
    }

    /**
     * Ensures that the id is friendly for a URL or file system
     *
     * @param id the unique id
     * @return the id as file friendly id
     */
    public static String generateSanitizedId(String id) {
        id = id.replace(':', '-');
        id = id.replace('_', '-');
        id = id.replace('.', '-');
        id = id.replace('/', '-');
        return id;
    }

        
    public static String getUUID() {
    	return getInstance().generateId();
    }
    
    public static UuidGenerator getInstance() {
    	if (instance == null) {
    		instance = new UuidGenerator(); 
    	}
    	return instance;
    }
  
    /**
     * When using the {@link java.net.InetAddress#getHostName()} method in an
     * environment where neither a proper DNS lookup nor an <tt>/etc/hosts</tt>
     * entry exists for a given host, the following exception will be thrown:
     * <code>
     * java.net.UnknownHostException: &lt;hostname&gt;: &lt;hostname&gt;
     *  at java.net.InetAddress.getLocalHost(InetAddress.java:1425)
     *   ...
     * </code>
     * Instead of just throwing an UnknownHostException and giving up, this
     * method grabs a suitable hostname from the exception and prevents the
     * exception from being thrown. If a suitable hostname cannot be acquired
     * from the exception, only then is the <tt>UnknownHostException</tt> thrown.
     *
     * @return The hostname
     * @throws UnknownHostException
     * @see {@link java.net.InetAddress#getLocalHost()}
     * @see {@link java.net.InetAddress#getHostName()}
     */
    static String getLocalHostName() throws UnknownHostException {
        try {
            return (InetAddress.getLocalHost()).getHostName();
        } catch (UnknownHostException uhe) {
            String host = uhe.getMessage(); // host = "hostname: hostname"
            if (host != null) {
                int colon = host.indexOf(':');
                if (colon > 0) {
                    return host.substring(0, colon);
                }
            }
            throw uhe;
        }
    }
}
