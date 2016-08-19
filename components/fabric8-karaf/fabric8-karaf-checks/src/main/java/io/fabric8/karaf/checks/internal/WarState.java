/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.karaf.checks.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.fabric8.karaf.checks.Check;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;

public class WarState extends AbstractBundleChecker
                            implements WebListener {

    private final Map<Long, WebEvent> states = new ConcurrentHashMap<>();

    public WarState() {
        bundleContext.registerService(WebListener.class, this, null);
    }

    @Override
    public void webEvent(WebEvent event) {
        states.put(event.getBundle().getBundleId(), event);
    }

    @Override
    public Check checkBundle(Bundle bundle) {
        WebEvent event = states.get(bundle.getBundleId());
        if (event != null && event.getType() != WebEvent.DEPLOYED && isActive(bundle)) {
            return new Check("war-state", "War bundle " + bundle.getBundleId() + " is in state " + getState(event));
        }
        return null;
    }

    private String getState(WebEvent webEvent) {
        switch (webEvent.getType()) {
            case WebEvent.DEPLOYED:
                return "DEPLOYED";
            case WebEvent.DEPLOYING:
                return "DEPLOYING";
            case WebEvent.FAILED:
                return "FAILED";
            case WebEvent.UNDEPLOYED:
                return "UNDEPLOYED";
            case WebEvent.UNDEPLOYING:
                return "UNDEPLOYING";
            case WebEvent.WAITING:
                return "WAITING";
            default:
                return "UNKNOWN";
        }
    }

}
