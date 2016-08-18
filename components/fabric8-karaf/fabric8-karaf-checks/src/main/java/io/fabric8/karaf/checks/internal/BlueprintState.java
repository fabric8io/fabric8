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
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;

public class BlueprintState extends AbstractBundleChecker
                            implements BlueprintListener {

    private final Map<Long, BlueprintEvent> states = new ConcurrentHashMap<>();

    public BlueprintState() {
        bundleContext.registerService(BlueprintListener.class, this, null);
    }

    @Override
    public void blueprintEvent(BlueprintEvent event) {
        states.put(event.getBundle().getBundleId(), event);
    }

    @Override
    public Check checkBundle(Bundle bundle) {
        BlueprintEvent event = states.get(bundle.getBundleId());
        if (event != null && event.getType() != BlueprintEvent.CREATED && isActive(bundle)) {
            return new Check("blueprint-state", "Blueprint bundle " + bundle.getBundleId() + " is in state " + getState(event));
        }
        return null;
    }

    private String getState(BlueprintEvent blueprintEvent) {
        switch (blueprintEvent.getType()) {
            case BlueprintEvent.CREATING:
                return "CREATING";
            case BlueprintEvent.CREATED:
                return "CREATED";
            case BlueprintEvent.DESTROYING:
                return "DESTROYING";
            case BlueprintEvent.DESTROYED:
                return "DESTROYED";
            case BlueprintEvent.FAILURE:
                return "FAILURE";
            case BlueprintEvent.GRACE_PERIOD:
                return "GRACE_PERIOD";
            case BlueprintEvent.WAITING:
                return "WAITING";
            default:
                return "UNKNOWN";
        }
    }

}
