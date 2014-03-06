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
package io.fabric8.jaas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import org.osgi.framework.BundleContext;

@ThreadSafe
@Component(name = "io.fabric8.jaas", label = "%Fabric8 Jaas Realm", //label = "Security realm using Fabric8",
        policy = ConfigurationPolicy.OPTIONAL, immediate = false, metatype = true)
@Service(JaasRealm.class)
@Properties(
        @Property(name = "supports.container.tokens", value = "true", propertyPrivate=true)
)
public final class FabricJaasRealm extends AbstractComponent implements JaasRealm {

    private static final String REALM = "karaf";
    private static final String ZK_LOGIN_MODULE = "io.fabric8.jaas.ZookeeperLoginModule";

    private static final String PATH = "path";
    private static final String ENCRYPTION_NAME = "encryption.name";
    private static final String ENCRYPTION_ENABLED = "encryption.enabled";
    private static final String ENCRYPTION_PREFIX = "encryption.prefix";
    private static final String ENCRYPTION_SUFFIX = "encryption.suffix";
    private static final String ENCRYPTION_ALGORITHM = "encryption.algorithm";
    private static final String ENCRYPTION_ENCODING = "encryption.encoding";
    private static final String MODULE = "org.apache.karaf.jaas.module";

    @Property(name = MODULE, label = "Login Module Class", value = ZK_LOGIN_MODULE)
    private String module;
    @Property(name = ENCRYPTION_NAME, label="Encryption Service Name", description = "The encryption service name. Defaults to basic, a more powerful alternative is jasypt", value = "basic")
    private String encryptionName;
    @Property(name = ENCRYPTION_ENABLED, label = "Encryption Enabled", description = "Flag that enables encryption", boolValue = true)
    private Boolean encryptionEnabled;
    @Property(name = ENCRYPTION_PREFIX,  label = "Encryption Prefix", description = "The encrypted password will be prefixed with that value", value = "{CRYPT}")
    private String encryptionPrefix;
    @Property(name = ENCRYPTION_SUFFIX, label = "Encryption Suffix",  description = "The encrypted password will be suffixed with that value", value = "{CRYPT}")
    private String encryptionSuffix;
    @Property(name = ENCRYPTION_ALGORITHM, label = "Encryption Algorithm",  description = "The encryption algorithm to use on password", value = "MD5")
    private String encryptionAlgorithm;
    @Property(name = ENCRYPTION_ENCODING, label = "Encryption Encoding", description = "The encryption encoding to use on password", value = "hexadecimal")
    private String encryptionEncoding;
    @Property(name = PATH, label = "Users Path", description = "The path property to pass to the login module",  value = "/fabric/authentication/users")
    private String path;

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private final List<AppConfigurationEntry> enties = new ArrayList<AppConfigurationEntry>();


    @Activate
    void activate(BundleContext bundleContext, Map<String, Object> configuration) {
        Map<String, Object> options = new HashMap<String, Object>();
        options.putAll(configuration);
        options.put(BundleContext.class.getName(), bundleContext);
        options.put(ProxyLoginModule.PROPERTY_MODULE, ZK_LOGIN_MODULE);
        options.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));
        enties.add(new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options));
        activateComponent();
    }

    @Modified
    void modified(BundleContext bundleContext, Map<String, Object> configuration) {
        Map<String, Object> options = new HashMap<String, Object>();
        options.putAll(configuration);
        options.put(BundleContext.class.getName(), bundleContext);
        options.put(ProxyLoginModule.PROPERTY_MODULE, ZK_LOGIN_MODULE);
        options.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));
        synchronized (enties) {
            enties.clear();
            enties.add(new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options));
        }
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getName() {
        assertValid();
        return REALM;
    }

    @Override
    public int getRank() {
        assertValid();
        return 1;
    }

    @Override
    public AppConfigurationEntry[] getEntries() {
        assertValid();
        synchronized (enties) {
            return enties.toArray(new AppConfigurationEntry[enties.size()]);
        }
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }
}
