/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.zeroconf;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jmdns.JmDNS;

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
            tracker.jmDNS = JmDNS.create(address);
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
