/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.api;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;

public final class ServiceLocator {

	public static final Long DEFAULT_TIMEOUT = 60000L;

	private ServiceLocator() {
		//Utility Class
	}

    public static <T> T awaitService(Class<T> type) {
        return awaitService(type, null, DEFAULT_TIMEOUT);
    }

	public static <T> T awaitService(Class<T> type, long timeout) {
		return awaitService(type, null, timeout);
	}

    public static <T> T awaitService(Class<T> type, String filter) {
        return awaitService(type, filter, DEFAULT_TIMEOUT);
    }

    public static <T> T awaitService(Class<T> type, String filter, long timeout) {
		BundleContext bundleContext = getBundleContext();
		ServiceTracker<T, T> tracker = null;
		try {
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
			Filter osgiFilter = FrameworkUtil.createFilter(flt);
			tracker = new ServiceTracker<T, T>(bundleContext, osgiFilter, null);
			tracker.open(true);
			// Note that the tracker is not closed to keep the reference
			// This is buggy, as the service reference may change i think
			Object svc = type.cast(tracker.waitForService(timeout));
			if (svc == null) {
				Dictionary<String, String> dic = bundleContext.getBundle().getHeaders();
				System.err.println("Test bundle headers: " + explode(dic));

				for (ServiceReference<?> ref : asCollection(bundleContext.getAllServiceReferences(null, null))) {
					System.err.println("ServiceReference: " + ref);
				}

				for (ServiceReference<?> ref : asCollection(bundleContext.getAllServiceReferences(null, flt))) {
					System.err.println("Filtered ServiceReference: " + ref);
				}

				throw new RuntimeException("Gave up waiting for service " + flt);
			}
			return type.cast(svc);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Invalid filter", e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the bundle context.
	 */
	private static BundleContext getBundleContext() {
		return FrameworkUtil.getBundle(ServiceLocator.class).getBundleContext();
	}

	/**
	 * Explode the dictionary into a ,-delimited list of key=value pairs
	 */
	private static String explode(Dictionary<String, String> dictionary) {
		Enumeration<String> keys = dictionary.keys();
		StringBuffer result = new StringBuffer();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			result.append(String.format("%s=%s", key, dictionary.get(key)));
			if (keys.hasMoreElements()) {
				result.append(", ");
			}
		}
		return result.toString();
	}

	/**
	 * Provides an iterable collection of references, even if the original array is null
	 */
	private static Collection<ServiceReference<?>> asCollection(ServiceReference<?>[] references) {
		return references != null ? Arrays.asList(references) : Collections.<ServiceReference<?>>emptyList();
	}
}
