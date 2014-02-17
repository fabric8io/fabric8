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
package io.fabric8.itests.paxexam.support;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A {@link java.util.concurrent.Callable} that waits for the {@link io.fabric8.api.Container} to provision.
 */
public class WaitForServiceAddingTask<T> implements Callable<T> {


    private final Class<T> type;
    private final String filter;
    private final ServiceTracker<T, T> tracker;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final BundleContext bundleContext = FrameworkUtil.getBundle(WaitForServiceAddingTask.class).getBundleContext();

    public WaitForServiceAddingTask(Class<T> type, String filter) throws Exception {
        this.type = type;
        this.filter = filter;

        this.tracker = createTracker(type, filter, latch);
        this.tracker.open(true);
    }

    private ServiceTracker<T, T> createTracker(Class<T> type, String filter, final CountDownLatch latch) throws InvalidSyntaxException {
        ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(bundleContext, createFilter(type, filter), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                latch.countDown();
                return super.addingService(reference);
            }
        };
        return tracker;
    }

    private static <T> Filter createFilter(Class<T> type, String filter) throws InvalidSyntaxException {
        String flt;
        if (filter != null) {
            if (filter.startsWith("(")) {
                flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
            } else {
                flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
            }
        } else {
            flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
        }
        return FrameworkUtil.createFilter(flt);
    }


    @Override
    public T call() throws Exception {
        latch.await();
        return tracker.getService();
    }
}
