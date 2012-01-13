/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zeroconf;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.activemq.jmdns.JmDNS;

public final class JmDNSFactory {

    static Map<InetAddress, UsageTracker> registry = new HashMap<InetAddress, UsageTracker>();

    static class UsageTracker {
        AtomicInteger count = new AtomicInteger(0);
        JmDNS jmDNS;
    }

    private JmDNSFactory() {
    }

    static synchronized JmDNS create(final InetAddress address) throws IOException {
        UsageTracker tracker = registry.get(address);
        if (tracker == null) {
            tracker = new UsageTracker();
            tracker.jmDNS = new JmDNS(address) {
                public void close() {
                    if (onClose(address)) {
                        super.close();
                    }
                }
            };
            registry.put(address, tracker);
        }
        tracker.count.incrementAndGet();
        return tracker.jmDNS;
    }

    static synchronized boolean onClose(InetAddress address) {
        UsageTracker tracker = registry.get(address);
        if (tracker != null) {
            if (tracker.count.decrementAndGet() == 0) {
                registry.remove(address);
                return true;
            }
        }
        return false;
    }
}
