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
package io.fabric8.utils;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ports {

    public static final int DEFAULT_HOST_SSH_PORT = 22;
    public static final int DEFAULT_KARAF_SSH_PORT = 8101;
    public static final int DEFAULT_ZOOKEEPER_SERVER_PORT = 2181;
    public static final int DEFAULT_ZOOKEEPER_PEER_PORT = 2888;
    public static final int DEFAULT_ZOOKEEPER_ELECTION_PORT = 3888;
    public static final int DEFAULT_RMI_SERVER_PORT = 44444;
    public static final int DEFAULT_RMI_REGISTRY_PORT = 1099;
    public static final int DEFAULT_HTTP_PORT = 8181;
    public static final int DEFAULT_HTTPS_PORT = 8443;


    public static final int MIN_PORT_NUMBER = 0;
    public static final int MAX_PORT_NUMBER = 65535;
    public static final String PORT_PATTERN = ":[\\d]*$";


    private Ports() {
        //Utility Class
    }


    /**
     * Returns a {@link Set} of used ports within the range.
     * @param fromPort
     * @param toPort
     * @return
     */
    public static Set<Integer> findUsedPorts(int fromPort, int toPort) {
        Set<Integer> usedPorts = new HashSet<Integer>();
        for (int port = fromPort; port <= toPort; port++) {
            if (!isPortFree(port)) {
                usedPorts.add(port);
            }
        }
        return usedPorts;
    }

    /**
     * Finds a port based on the given ip and the used ports for that ip.
     *
     * @param usedPorts
     * @param ip
     * @param port
     * @return
     */
    public static int findPort(Map<String, List<Integer>> usedPorts, String ip, int port) {
        List<Integer> ports = usedPorts.get(ip);
        if (ports == null) {
            ports = new ArrayList<Integer>();
            usedPorts.put(ip, ports);
        }
        for (; ; ) {
            if (!ports.contains(port)) {
                ports.add(port);
                return port;
            }
            port++;
        }
    }

    /**
     * Finds a the next free local port, based on the list of used ports and the ability to directly check if port is free.
     *
     * @param usedPorts
     * @param fromPort
     * @param toPort
     * @param checkIfAvailable
     * @return
     */
    public static int findFreeLocalPort(Set<Integer> usedPorts, int fromPort, int toPort, boolean checkIfAvailable) {
        for (int port = fromPort; port < toPort; port++) {
            if (checkIfAvailable && !isPortFree(port)) {
                continue;
            } else if (!usedPorts.contains(port)) {
                return port;
            }
        }
        throw new RuntimeException("No port available within range");
    }

    /**
     * Maps the target port inside a port range.
     *
     * @param port
     * @param minimumPort
     * @param maximumPort
     * @return
     */
    public static int mapPortToRange(int port, int minimumPort, int maximumPort) {
        if (maximumPort == 0) {
            maximumPort = MAX_PORT_NUMBER;
        }
        if (minimumPort >= maximumPort) {
            return port;
        } else if (minimumPort == 0 && port <= minimumPort) {
            return port;
        } else if (port >= minimumPort && port <= maximumPort) {
            return port;
        } else {
            return port % (maximumPort - minimumPort) + minimumPort;
        }
    }

    /**
     * Maps the target port inside a port range.
     *
     * @param port
     * @param minimumPort
     * @param maximumPort
     * @return
     */
    public static int mapPortToRange(int port, String minimumPort, String maximumPort) {
        int min = 0;
        int max = 0;
        if (minimumPort != null) {
            try {
                min = Integer.parseInt(minimumPort);
            } catch (NumberFormatException e) {
                min = 0;
            }
        }

        if (maximumPort != null) {
            try {
                max = Integer.parseInt(maximumPort);
            } catch (NumberFormatException e) {
                max = 0;
            }
        }
        return mapPortToRange(port, min, max);
    }


    /**
     * Checks if a local port is free.
     *
     * @param port
     * @return
     */
    public static boolean isPortFree(int port) {
        if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }

    /**
     * Extracts the port from an addrees.
     *
     * @param address
     * @return
     */
    public static int extractPort(String address) {
        Pattern p = Pattern.compile(PORT_PATTERN);
        Matcher m = p.matcher(address);
        if (m.find()) {
            String match = m.group().substring(1);
            return Integer.parseInt(match);
        }
        return 0;
    }
}
