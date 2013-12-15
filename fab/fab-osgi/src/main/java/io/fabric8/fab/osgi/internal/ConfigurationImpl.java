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
package io.fabric8.fab.osgi.internal;

import io.fabric8.fab.MavenResolver;
import io.fabric8.fab.MavenResolverImpl;
import io.fabric8.fab.osgi.ServiceConstants;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.ops4j.util.property.PropertyResolver;
import org.ops4j.util.property.PropertyStore;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;

import static io.fabric8.fab.osgi.util.ConfigurationAdminHelper.getProperties;

public class ConfigurationImpl extends PropertyStore implements Configuration {
    private PropertyResolver propertyResolver;

    public ConfigurationImpl(PropertyResolver propertyResolver) {
        NullArgumentException.validateNotNull(propertyResolver, "PropertyResolver");
        this.propertyResolver = propertyResolver;
    }

    public static ConfigurationImpl newInstance() {
        PropertiesPropertyResolver resolver = new PropertiesPropertyResolver(System.getProperties());
        return new ConfigurationImpl(resolver);
    }

    public static ConfigurationImpl newInstance(ConfigurationAdmin admin, BundleContext context) {
        PropertyResolver resolver =
                new DictionaryPropertyResolver(getProperties(admin, ServiceConstants.PID),
                        new DictionaryPropertyResolver(getProperties(admin, "org.ops4j.pax.url.mvn"),
                                new BundleContextPropertyResolver(context)));

        return new ConfigurationImpl(resolver);
    }

    /**
     * Returns true if the certificate should be checked on SSL connection, false otherwise
     */
    public boolean getCertificateCheck() {
        if (!contains(ServiceConstants.PROPERTY_CERTIFICATE_CHECK)) {
            return set(ServiceConstants.PROPERTY_CERTIFICATE_CHECK,
                    Boolean.valueOf(propertyResolver.get(ServiceConstants.PROPERTY_CERTIFICATE_CHECK))
            );
        }
        return (Boolean) get(ServiceConstants.PROPERTY_CERTIFICATE_CHECK);
    }

    /**
     * Returns whether or not the shared dependencies should be installed
     */
    public boolean isInstallMissingDependencies() {
        if (!contains(ServiceConstants.PROPERTY_INSTALL_PROVIDED_DEPENDENCIES)) {
            String value = propertyResolver.get(ServiceConstants.PROPERTY_INSTALL_PROVIDED_DEPENDENCIES);
            if (value != null) {
                Boolean aBoolean = Boolean.valueOf(value);
                return set(ServiceConstants.PROPERTY_INSTALL_PROVIDED_DEPENDENCIES,
                        aBoolean
                );
            }
        }
        Boolean answer = get(ServiceConstants.PROPERTY_INSTALL_PROVIDED_DEPENDENCIES);
        if (answer == null) {
            return ServiceConstants.DEFAULT_INSTALL_PROVIDED_DEPENDENCIES;
        } else {
            return answer.booleanValue();
        }
    }

    public MavenResolver getResolver() {
        MavenResolverImpl resolver = new MavenResolverImpl();
        String[] repositories = getMavenRepositories();
        if (repositories != null) {
            resolver.setRepositories(repositories);
        }
        String localrepo = getLocalMavenRepository();
        if (localrepo != null) {
            resolver.setLocalRepo(localrepo);
        }
        return resolver;
    }

    public String[] getMavenRepositories() {
        if (!contains(ServiceConstants.PROPERTY_MAVEN_REPOSITORIES)) {
            String text = propertyResolver.get(ServiceConstants.PROPERTY_MAVEN_REPOSITORIES);
            String[] repositories = toArray(text);
            return set(ServiceConstants.PROPERTY_MAVEN_REPOSITORIES, repositories);
        }
        return get(ServiceConstants.PROPERTY_MAVEN_REPOSITORIES);
    }

    public String getLocalMavenRepository() {
        if (!contains(ServiceConstants.PROPERTY_LOCAL_MAVEN_REPOSITORY)) {
            String text = propertyResolver.get(ServiceConstants.PROPERTY_LOCAL_MAVEN_REPOSITORY);
            return set(ServiceConstants.PROPERTY_LOCAL_MAVEN_REPOSITORY, text);
        }
        return get(ServiceConstants.PROPERTY_LOCAL_MAVEN_REPOSITORY);
    }

    public String[] getSharedResourcePaths() {
        if (!contains(ServiceConstants.PROPERTY_SHARED_RESOURCE_PATHS)) {
            String text = propertyResolver.get(ServiceConstants.PROPERTY_SHARED_RESOURCE_PATHS);
            String[] repositories;
            if (text == null || text.length() == 0) {
                repositories = ServiceConstants.DEFAULT_PROPERTY_SHARED_RESOURCE_PATHS;
            } else {
                repositories = toArray(text);
            }
            return set(ServiceConstants.PROPERTY_SHARED_RESOURCE_PATHS, repositories);
        }
        return get(ServiceConstants.PROPERTY_SHARED_RESOURCE_PATHS);
    }


    protected static String[] toArray(String text) {
        String[] answer = null;
        if (text != null) {
            answer = text.split(",");
        }
        return answer;
    }

    protected PropertyResolver getPropertyResolver() {
        return propertyResolver;
    }
}