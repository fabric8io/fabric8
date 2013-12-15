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

package io.fabric8.dosgi.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.fabric8.dosgi.capset.Attribute;
import io.fabric8.dosgi.capset.Capability;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import static org.osgi.service.remoteserviceadmin.RemoteConstants.*;

/**
 * A description of an endpoint that provides sufficient information for a
 * compatible distribution provider to create a connection to this endpoint
 * 
 * An Endpoint Description is easy to transfer between different systems because
 * it is property based where the property keys are strings and the values are
 * simple types. This allows it to be used as a communications device to convey
 * available endpoint information to nodes in a network.
 * 
 * An Endpoint Description reflects the perspective of an <i>importer</i>. That
 * is, the property keys have been chosen to match filters that are created by
 * client bundles that need a service. Therefore the map must not contain any
 * <code>service.exported.*</code> property and must contain the corresponding
 * <code>service.imported.*</code> ones.
 * 
 * The <code>service.intents</code> property must contain the intents provided
 * by the service itself combined with the intents added by the exporting
 * distribution provider. Qualified intents appear fully expanded on this
 * property.
 * 
 * @Immutable
 * @version $Revision: 8645 $
 */

public class EndpointDescription implements Capability {

	private final Map<String, Object>	properties;
	private final List<String>			interfaces;
	private final long					serviceId;
	private final String				frameworkUUID;
	private final String				id;

	/**
	 * Create an Endpoint Description from a Map.
	 * 
	 * <p>
	 * The {@link RemoteConstants#ENDPOINT_ID endpoint.id},
	 * {@link RemoteConstants#SERVICE_IMPORTED_CONFIGS service.imported.configs}
	 * and <code>objectClass</code> properties must be set.
	 * 
	 * @param properties The map from which to create the Endpoint Description.
	 *        The keys in the map must be type <code>String</code> and, since
	 *        the keys are case insensitive, there must be no duplicates with
	 *        case variation.
	 * @throws IllegalArgumentException When the properties are not proper for
	 *         an Endpoint Description.
	 */

	public EndpointDescription(Map<String, Object> properties) {
		Map<String, Object> props = new TreeMap<String, Object>(
				String.CASE_INSENSITIVE_ORDER);
		try {
			props.putAll(properties);
		}
		catch (ClassCastException e) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"non-String key in properties");
			iae.initCause(e);
			throw iae;
		}
		if (props.size() < properties.size()) {
			throw new IllegalArgumentException(
					"duplicate keys with different cases in properties: "
							+ new ArrayList<String>(props.keySet())
									.removeAll(properties.keySet()));
		}

		conditionProperties(props);
		this.properties = Collections.unmodifiableMap(props);
		/* properties must be initialized before calling the following methods */
		interfaces = verifyObjectClassProperty();
		serviceId = verifyLongProperty(ENDPOINT_SERVICE_ID);
		frameworkUUID = verifyStringProperty(ENDPOINT_FRAMEWORK_UUID);
		id = verifyStringProperty(ENDPOINT_ID).trim();
		if (id == null) {
			throw new IllegalArgumentException(ENDPOINT_ID
					+ " property must be set");
		}
		if (getConfigurationTypes().isEmpty()) {
			throw new IllegalArgumentException(SERVICE_IMPORTED_CONFIGS
					+ " property must be set and non-empty");
		}
	}

	/**
	 * Create an Endpoint Description based on a Service Reference and a Map of
	 * properties. The properties in the map take precedence over the properties
	 * in the Service Reference.
	 * 
	 * <p>
	 * This method will automatically set the
	 * {@link RemoteConstants#ENDPOINT_FRAMEWORK_UUID endpoint.framework.uuid}
	 * and {@link RemoteConstants#ENDPOINT_SERVICE_ID endpoint.service.id}
	 * properties based on the specified Service Reference as well as the
	 * {@link RemoteConstants#SERVICE_IMPORTED service.imported} property if
	 * they are not specified as properties.
	 * <p>
	 * The {@link RemoteConstants#ENDPOINT_ID endpoint.id},
	 * {@link RemoteConstants#SERVICE_IMPORTED_CONFIGS service.imported.configs}
	 * and <code>objectClass</code> properties must be set.
	 * 
	 * @param reference A service reference that can be exported.
	 * @param properties Map of properties. This argument can be
	 *        <code>null</code>. The keys in the map must be type
	 *        <code>String</code> and, since the keys are case insensitive,
	 *        there must be no duplicates with case variation.
	 * @throws IllegalArgumentException When the properties are not proper for
	 *         an Endpoint Description
	 */
	public EndpointDescription(final ServiceReference reference,
			final Map<String, Object> properties) {
		Map<String, Object> props = new TreeMap<String, Object>(
				String.CASE_INSENSITIVE_ORDER);

		if (properties != null) {
			try {
				props.putAll(properties);
			}
			catch (ClassCastException e) {
				IllegalArgumentException iae = new IllegalArgumentException(
						"non-String key in properties");
				iae.initCause(e);
				throw iae;
			}
			if (props.size() < properties.size()) {
				throw new IllegalArgumentException(
						"duplicate keys with different cases in properties: "
								+ new ArrayList<String>(props.keySet())
										.removeAll(properties.keySet()));
			}
		}

		for (String key : reference.getPropertyKeys()) {
			if (!props.containsKey(key)) {
				props.put(key, reference.getProperty(key));
			}
		}

		if (!props.containsKey(ENDPOINT_SERVICE_ID)) {
			props.put(ENDPOINT_SERVICE_ID, reference.getProperty(Constants.SERVICE_ID));
		}
		if (!props.containsKey(ENDPOINT_FRAMEWORK_UUID)) {
			String uuid = null;
			try {
				uuid = AccessController
						.doPrivileged(new PrivilegedAction<String>() {
							public String run() {
								return reference.getBundle().getBundleContext()
										.getProperty("org.osgi.framework.uuid");
							}
						});
			}
			catch (SecurityException e) {
				// if we don't have permission, we can't get the property
			}
			if (uuid != null) {
				props.put(ENDPOINT_FRAMEWORK_UUID, uuid);
			}
		}
		conditionProperties(props);
		this.properties = Collections.unmodifiableMap(props);
		/* properties must be initialized before calling the following methods */
		interfaces = verifyObjectClassProperty();
		serviceId = verifyLongProperty(ENDPOINT_SERVICE_ID);
		frameworkUUID = verifyStringProperty(ENDPOINT_FRAMEWORK_UUID);
		id = verifyStringProperty(ENDPOINT_ID).trim();
		if (id == null) {
			throw new IllegalArgumentException(ENDPOINT_ID
					+ " property must be set");
		}
		if (getConfigurationTypes().isEmpty()) {
			throw new IllegalArgumentException(SERVICE_IMPORTED_CONFIGS
					+ " property must be set and non-empty");
		}
	}

	private static final String	SERVICE_EXPORTED_	= "service.exported.";
	private static final int	SERVICE_EXPORTED_length	= SERVICE_EXPORTED_
																.length();

	/**
	 * Condition the properties.
	 * 
	 * @param props Property map to condition.
	 */
	private void conditionProperties(Map<String, Object> props) {
		// ensure service.imported is set
		if (!props.containsKey(SERVICE_IMPORTED)) {
			props.put(SERVICE_IMPORTED, Boolean.toString(true));
		}

		// remove service.exported.* properties
		for (Iterator<String> iter = props.keySet().iterator(); iter.hasNext();) {
			String key = iter.next();
			if (SERVICE_EXPORTED_.regionMatches(true, 0, key, 0,
					SERVICE_EXPORTED_length)) {
				iter.remove();
			}
		}
	}

	/**
	 * Verify and obtain the interface list from the properties.
	 * 
	 * @return A list with the interface names.
	 * @throws IllegalArgumentException If the objectClass property is not set
	 *         or is empty or if the package version property values are
	 *         malformed.
	 */
	private List<String> verifyObjectClassProperty() {
		Object o = properties.get(Constants.OBJECTCLASS);
		if (!(o instanceof String[])) {
			throw new IllegalArgumentException(
					"objectClass value must be of type String[]");
		}
		String[] objectClass = (String[]) o;
		if (objectClass.length < 1) {
			throw new IllegalArgumentException("objectClass is empty");
		}
		for (String interf : objectClass) {
			int index = interf.lastIndexOf('.');
			if (index == -1) {
				continue;
			}
			String packageName = interf.substring(0, index);
			try {
				/* Make sure any package version properties are well formed */
				getPackageVersion(packageName);
			}
			catch (IllegalArgumentException e) {
				IllegalArgumentException iae = new IllegalArgumentException(
						"Improper version for package " + packageName);
				iae.initCause(e);
				throw iae;
			}
		}
		return Collections.unmodifiableList(Arrays.asList(objectClass));
	}

	/**
	 * Verify and obtain a required String property.
	 * 
	 * @param propName The name of the property
	 * @return The value of the property or <code>null</code> if the property is
	 *         not set.
	 * @throws IllegalArgumentException when the property doesn't have the
	 *         correct data type.
	 */
	private String verifyStringProperty(String propName) {
		Object r = properties.get(propName);
		try {
			return (String) r;
		}
		catch (ClassCastException e) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"property value is not a String: " + propName);
			iae.initCause(e);
			throw iae;
		}
	}

	/**
	 * Verify and obtain a required long property.
	 * 
	 * @param propName The name of the property
	 * @return The value of the property or 0 if the property is not set.
	 * @throws IllegalArgumentException when the property doesn't have the
	 *         correct data type.
	 */
	private long verifyLongProperty(String propName) {
		Object r = properties.get(propName);
		if (r == null) {
			return 0l;
		}
		try {
			return ((Long) r).longValue();
		}
		catch (ClassCastException e) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"property value is not a Long: " + propName);
			iae.initCause(e);
			throw iae;
		}
	}

	/**
	 * Returns the endpoint's id.
	 * 
	 * The id is an opaque id for an endpoint. No two different endpoints must
	 * have the same id. Two Endpoint Descriptions with the same id must
	 * represent the same endpoint.
	 * 
	 * The value of the id is stored in the {@link RemoteConstants#ENDPOINT_ID}
	 * property.
	 * 
	 * @return The id of the endpoint, never <code>null</code>. The returned
	 *         value has leading and trailing whitespace removed.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Provide the list of interfaces implemented by the exported service.
	 * 
	 * The value of the interfaces is derived from the <code>objectClass</code>
	 * property.
	 * 
	 * @return An unmodifiable list of Java interface names implemented by this
	 *         endpoint.
	 */
	public List<String> getInterfaces() {
		return interfaces;
	}

	/**
	 * Provide the version of the given package name.
	 * 
	 * The version is encoded by prefixing the given package name with
	 * {@link RemoteConstants#ENDPOINT_PACKAGE_VERSION_
	 * endpoint.package.version.}, and then using this as an endpoint property
	 * key. For example:
	 * 
	 * <pre>
	 * endpoint.package.version.com.acme
	 * </pre>
	 * 
	 * The value of this property is in String format and will be converted to a
	 * <code>Version</code> object by this method.
	 * 
	 * @param packageName The name of the package for which a version is
	 *        requested.
	 * @return The version of the specified package or
	 *         <code>Version.emptyVersion</code> if the package has no version
	 *         in this Endpoint Description.
	 * @throws IllegalArgumentException If the version property value is not
	 *         String.
	 */
	public Version getPackageVersion(String packageName) {
		String key = ENDPOINT_PACKAGE_VERSION_ + packageName;
		Object value = properties.get(key);
		String version;
		try {
			version = (String) value;
		}
		catch (ClassCastException e) {
			IllegalArgumentException iae = new IllegalArgumentException(key
					+ " property value is not a String");
			iae.initCause(e);
			throw iae;
		}
		return Version.parseVersion(version);
	}

	/**
	 * Returns the service id for the service exported through this endpoint.
	 * 
	 * This is the service id under which the framework has registered the
	 * service. This field together with the Framework UUID is a globally unique
	 * id for a service.
	 * 
	 * The value of the remote service id is stored in the
	 * {@link RemoteConstants#ENDPOINT_SERVICE_ID} endpoint property.
	 * 
	 * @return Service id of a service or 0 if this Endpoint Description does
	 *         not relate to an OSGi service.
	 * 
	 */
	public long getServiceId() {
		return serviceId;
	}

	/**
	 * Returns the configuration types.
	 * 
	 * A distribution provider exports a service with an endpoint. This endpoint
	 * uses some kind of communications protocol with a set of configuration
	 * parameters. There are many different types but each endpoint is
	 * configured by only one configuration type. However, a distribution
	 * provider can be aware of different configuration types and provide
	 * synonyms to increase the change a receiving distribution provider can
	 * create a connection to this endpoint.
	 * 
	 * This value of the configuration types is stored in the
	 * {@link RemoteConstants#SERVICE_IMPORTED_CONFIGS} service property.
	 * 
	 * @return An unmodifiable list of the configuration types used for the
	 *         associated endpoint and optionally synonyms.
	 */
	public List<String> getConfigurationTypes() {
		return getStringPlusProperty(SERVICE_IMPORTED_CONFIGS);
	}

	/**
	 * Return the list of intents implemented by this endpoint.
	 * 
	 * The intents are based on the service.intents on an imported service,
	 * except for any intents that are additionally provided by the importing
	 * distribution provider. All qualified intents must have been expanded.
	 * 
	 * This value of the intents is stored in the
	 * {@link RemoteConstants#SERVICE_INTENTS} service property.
	 * 
	 * @return An unmodifiable list of expanded intents that are provided by
	 *         this endpoint.
	 */
	public List<String> getIntents() {
		return getStringPlusProperty(SERVICE_INTENTS);
	}

	/**
	 * Reads a 'String+' property from the properties map, which may be of type
	 * String, String[] or Collection&lt;String&gt; and returns it as an
	 * unmodifiable List.
	 * 
	 * @param key The property
	 * @return An unmodifiable list
	 */
	private List<String> getStringPlusProperty(String key) {
		Object value = properties.get(key);
		if (value == null) {
			return Collections.EMPTY_LIST;
		}

		if (value instanceof String) {
			return Collections.singletonList((String) value);
		}

		if (value instanceof String[]) {
			String[] values = (String[]) value;
			List<String> result = new ArrayList<String>(values.length);
			for (String v : values) {
				if (v != null) {
					result.add(v);
				}
			}
			return Collections.unmodifiableList(result);
		}

		if (value instanceof Collection< ? >) {
			Collection< ? > values = (Collection< ? >) value;
			List<String> result = new ArrayList<String>(values.size());
			for (Iterator< ? > iter = values.iterator(); iter.hasNext();) {
				Object v = iter.next();
				if (v instanceof String) {
					result.add((String) v);
				}
			}
			return Collections.unmodifiableList(result);
		}

		return Collections.EMPTY_LIST;
	}

	/**
	 * Return the framework UUID for the remote service, if present.
	 * 
	 * The value of the remote framework uuid is stored in the
	 * {@link RemoteConstants#ENDPOINT_FRAMEWORK_UUID} endpoint property.
	 * 
	 * @return Remote Framework UUID, or <code>null</code> if this endpoint is
	 *         not associated with an OSGi framework having a framework uuid.
	 */
	public String getFrameworkUUID() {
		return frameworkUUID;
	}

	/**
	 * Returns all endpoint properties.
	 * 
	 * @return An unmodifiable map referring to the properties of this Endpoint
	 *         Description.
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}

	/**
	 * Answers if this Endpoint Description refers to the same service instance
	 * as the given Endpoint Description.
	 * 
	 * Two Endpoint Descriptions point to the same service if they have the same
	 * id or their framework UUIDs and remote service ids are equal.
	 * 
	 * @param other The Endpoint Description to look at
	 * @return True if this endpoint description points to the same service as
	 *         the other
	 */
	public boolean isSameService(EndpointDescription other) {
		if (this.equals(other)) {
			return true;
		}

		if (this.getFrameworkUUID() == null) {
			return false;
		}

		return (this.getServiceId() == other.getServiceId())
				&& this.getFrameworkUUID().equals(
						other.getFrameworkUUID());
	}

	/**
	 * Returns a hash code value for the object.
	 * 
	 * @return An integer which is a hash code value for this object.
	 */
	public int hashCode() {
		return getId().hashCode();
	}

	/**
	 * Compares this <code>EndpointDescription</code> object to another object.
	 * 
	 * <p>
	 * An Endpoint Description is considered to be <b>equal to</b> another
	 * Endpoint Description if their ids are equal.
	 * 
	 * @param other The <code>EndpointDescription</code> object to be compared.
	 * @return <code>true</code> if <code>object</code> is a
	 *         <code>EndpointDescription</code> and is equal to this object;
	 *         <code>false</code> otherwise.
	 */
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof EndpointDescription)) {
			return false;
		}
		return getId().equals(
				((EndpointDescription) other).getId());
	}

	/**
	 * Tests the properties of this <code>EndpointDescription</code> against
	 * the given filter using a case insensitive match.
	 * 
	 * @param filter The filter to test.
	 * @return <code>true</code> If the properties of this
	 *         <code>EndpointDescription</code> match the filter,
	 *         <code>false</code> otherwise.
	 * @throws IllegalArgumentException If <code>filter</code> contains an
	 *         invalid filter string that cannot be parsed.
	 */
	public boolean matches(String filter) {
		Filter f;
		try {
			f = FrameworkUtil.createFilter(filter);
		}
		catch (InvalidSyntaxException e) {
			IllegalArgumentException iae = new IllegalArgumentException(e
					.getMessage());
			iae.initCause(e);
			throw iae;
		}
		Dictionary<String, Object> d = new UnmodifiableDictionary<String, Object>(
				properties);
		/*
		 * we can use matchCase here since properties already supports case
		 * insensitive key lookup.
		 */
		return f.matchCase(d);
	}

	/**
	 * Returns the string representation of this EndpointDescription.
	 * 
	 * @return String form of this EndpointDescription.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('{');
		Iterator<Map.Entry<String, Object>> iter = properties.entrySet()
				.iterator();
		boolean comma = false;
		while (iter.hasNext()) {
			Map.Entry<String, Object> entry = iter.next();
			if (comma) {
				sb.append(", ");
			}
			else {
				comma = true;
			}
			sb.append(entry.getKey());
			sb.append('=');
			Object value = entry.getValue();
			if (value != null) {
				Class< ? > valueType = value.getClass();
				if (Object[].class.isAssignableFrom(valueType)) {
					append(sb, (Object[]) value);
					continue;
				}
			}
			sb.append(value);
		}
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Append the specified Object array to the specified StringBuffer.
	 * 
	 * @param sb Receiving StringBuffer.
	 * @param value Object array to append to the specified StringBuffer.
	 */
	private static void append(StringBuffer sb, Object[] value) {
		sb.append('[');
		boolean comma = false;
		final int length = value.length;
		for (int i = 0; i < length; i++) {
			if (comma) {
				sb.append(", ");
			}
			else {
				comma = true;
			}
			sb.append(String.valueOf(value[i]));
		}
		sb.append(']');
	}

	/**
	 * Unmodifiable Dictionary wrapper for a Map. This class is also used by
	 * EndpointPermission.
	 */
	static class UnmodifiableDictionary<K, V> extends Dictionary<K, V> {
		private final Map<K, V>	wrapped;

		UnmodifiableDictionary(Map<K, V> wrapped) {
			this.wrapped = wrapped;
		}

		public Enumeration<V> elements() {
			return Collections.enumeration(wrapped.values());
		}

		public V get(Object key) {
			return wrapped.get(key);
		}

		public boolean isEmpty() {
			return wrapped.isEmpty();
		}

		public Enumeration<K> keys() {
			return Collections.enumeration(wrapped.keySet());
		}

		public V put(K key, V value) {
			throw new UnsupportedOperationException();
		}

		public V remove(Object key) {
			throw new UnsupportedOperationException();
		}

		public int size() {
			return wrapped.size();
		}

		public String toString() {
			return wrapped.toString();
		}
	}

    public Attribute getAttribute(String name) {
        Object val = properties.get(name);
        return val != null ? new Attribute( name, val ) : null;
    }
}
