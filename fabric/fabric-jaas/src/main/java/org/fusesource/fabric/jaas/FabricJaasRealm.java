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
package org.fusesource.fabric.jaas;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.fusesource.fabric.api.FabricService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.HashMap;
import java.util.Map;

@Component(name = "org.fusesource.fabric.jaas", description = "Fabric Jaas Realm")
@Service(JaasRealm.class)
public class FabricJaasRealm implements JaasRealm {

    private static final String REALM = "karaf";
    private static final String ZK_LOGIN_MODULE = "org.fusesource.fabric.jaas.ZookeeperLoginModule";

    private static final String PATH = "path";
    private static final String ENCRYPTION_NAME = "encryption.name";
    private static final String ENCRYPTION_ENABLED = "encryption.enabled";
    private static final String ENCRYPTION_PREFIX = "encryption.prefix";
    private static final String ENCRYPTION_SUFFIX = "encryption.suffix";
    private static final String ENCRYPTION_ALGORITHM = "encryption.algorithm";
    private static final String ENCRYPTION_ENCODING = "encryption.encoding";
    private static final String MODULE = "org.apache.karaf.jaas.module";

    @Property(name = MODULE, value = ZK_LOGIN_MODULE)
    private String module;
    @Property(name = ENCRYPTION_NAME, value = "")
    private String encryptionName;
    @Property(name = ENCRYPTION_ENABLED, boolValue = true)
    private Boolean encryptionEnabled;
    @Property(name = ENCRYPTION_PREFIX, value = "{CRYPT}")
    private String encryptionPrefix;
    @Property(name = ENCRYPTION_SUFFIX, value = "{CRYPT}")
    private String encryptionSuffix;
    @Property(name = ENCRYPTION_ALGORITHM, value = "MD5")
    private String encryptionAlgorithm;
    @Property(name = ENCRYPTION_ENCODING, value = "hexadecimal")
    private String encryptionEncoding;
    @Property(name = PATH, value = "/fabric/authentication/users")
    private String path;

    @Reference
    private CuratorFramework curator;
    private AppConfigurationEntry[] enties;

    @Activate
    public void init(BundleContext bundleContext, Map<String, Object> properties) {
        Map<String, Object> options = new HashMap<String, Object>();
        options.putAll(properties);
        options.put(BundleContext.class.getName(), bundleContext);
        options.put(ProxyLoginModule.PROPERTY_MODULE, ZK_LOGIN_MODULE);
        options.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));

        enties = new AppConfigurationEntry[1];
        enties[0] = new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
    }

    @Override
    public String getName() {
        return REALM;
    }

    @Override
    public int getRank() {
        return 1;
    }

    @Override
    public AppConfigurationEntry[] getEntries() {
        return enties;
    }
}
