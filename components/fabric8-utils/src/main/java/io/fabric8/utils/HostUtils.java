/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class HostUtils {
    private static final transient Logger LOG = LoggerFactory.getLogger(HostUtils.class);

    public static final String PREFERED_ADDRESS_PROPERTY_NAME = "preferred.network.address";

    private HostUtils() {
        //Utility Class
    }

    /**
     * Returns a {@link} of {@link InetAddress} per {@link NetworkInterface} as a {@link Map}.
     *
     * @return
     */
    public static Map<String, Set<InetAddress>> getNetworkInterfaceAddresses() {
        //JVM returns interfaces in a non-predictable order, so to make this more predictable
        //let's have them sort by interface name (by using a TreeMap).
        Map<String, Set<InetAddress>> interfaceAddressMap = new TreeMap<String, Set<InetAddress>>();
        try {
            Enumeration ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                //We only care about usable non-loopback interfaces.
                if (iface.isUp() && !iface.isLoopback()) {
                    String name = iface.getName();
                    Enumeration<InetAddress> ifaceAdresses = iface.getInetAddresses();
                    while (ifaceAdresses.hasMoreElements()) {
                        InetAddress ia = ifaceAdresses.nextElement();
                        //We want to filter out mac addresses
                        if (!ia.isLoopbackAddress() && ia.getHostAddress().indexOf(":") == -1) {
                            Set<InetAddress> addresses = interfaceAddressMap.get(name);
                            if (addresses == null) {
                                addresses = new LinkedHashSet<InetAddress>();
                            }
                            addresses.add(ia);
                            interfaceAddressMap.put(name, addresses);
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            //noop
        }
        return interfaceAddressMap;
    }

    /**
     * Returns a {@link Set} of {@link InetAddress} that are non-loopback or mac.
     * @return
     */
    public static Set<InetAddress> getAddresses() {
        Set<InetAddress> allAddresses = new LinkedHashSet<InetAddress>();
        Map<String, Set<InetAddress>> interfaceAddressMap = getNetworkInterfaceAddresses();
        for (Map.Entry<String, Set<InetAddress>> entry : interfaceAddressMap.entrySet()) {
            Set<InetAddress> addresses = entry.getValue();
            if (!addresses.isEmpty()) {
                for (InetAddress address : addresses) {
                    allAddresses.add(address);
                }
            }
        }
        return allAddresses;
    }


    /**
     * Chooses one of the available {@link InetAddress} based on the specified preference.
     * If the preferred address is not part of the available addresses it will be ignored.
     *
     * @param preferred
     * @return
     */
    private static InetAddress chooseAddress(String preferred) throws UnknownHostException {
        Set<InetAddress> addresses = getAddresses();
        if (preferred != null && !preferred.isEmpty()) {
            //Favor preferred address if exists
            try {
                InetAddress preferredAddress = InetAddress.getByName(preferred);
                if (addresses != null && addresses.contains(preferredAddress)) {
                    LOG.info("preferred address is " + preferredAddress.getHostAddress() + " for host " + preferredAddress.getHostName());
                    return preferredAddress;
                }
            } catch (UnknownHostException e) {
                //noop
            }
            for (InetAddress address : addresses) {
                if (preferred.equals(address.getHostName())) {
                    return address;
                }
            }
            StringBuffer hostNameBuffer = new StringBuffer();
            for (InetAddress address : addresses) {
                if (hostNameBuffer.length() > 0) {
                    hostNameBuffer.append(", ");
                }
                hostNameBuffer.append(address.getHostName() + "/" + address.getHostAddress());
            }
            LOG.warn("Could not find network address for preferred '" + preferred + "' when the addresses were: " + hostNameBuffer);
        }
        if (addresses.contains(InetAddress.getLocalHost())) {
            //Then if local host address is not bound to a loop-back interface, use it.
            return InetAddress.getLocalHost();
        } else if (addresses != null && !addresses.isEmpty()) {
            //else return the first available addrress
            return addresses.toArray(new InetAddress[addresses.size()])[0];
        } else {
            //else we are forcedt to use the localhost address.
            return InetAddress.getLocalHost();
        }
    }

    /**
     * Returns the local hostname. It loops through the network interfaces and returns the first non loopback address
     *
     * @return
     * @throws java.net.UnknownHostException
     */
    public static String getLocalHostName() throws UnknownHostException {
        String preffered = System.getProperty(PREFERED_ADDRESS_PROPERTY_NAME);
        return chooseAddress(preffered).getHostName();
    }

    /**
     * Returns the local IP. It loops through the network interfaces and returns the first non loopback address
     *
     * @return
     * @throws UnknownHostException
     */
    public static String getLocalIp() throws UnknownHostException {
        String preffered = System.getProperty(PREFERED_ADDRESS_PROPERTY_NAME);
        return chooseAddress(preffered).getHostAddress();
    }

}
