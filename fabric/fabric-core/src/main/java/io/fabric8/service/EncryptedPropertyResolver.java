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
package io.fabric8.service;

import static io.fabric8.zookeeper.ZkPath.AUTHENTICATION_CRYPT_ALGORITHM;
import static io.fabric8.zookeeper.ZkPath.AUTHENTICATION_CRYPT_PASSWORD;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.PlaceholderResolver;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;

import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

@ThreadSafe
@Component(name = "io.fabric8.placholder.resolver.crypt", label = "Fabric8 Encrypted Property Placeholder Resolver", metatype = false)
@Service({ PlaceholderResolver.class, EncryptedPropertyResolver.class })
@Properties({ @Property(name = "scheme", value = EncryptedPropertyResolver.RESOLVER_SCHEME) })
public final class EncryptedPropertyResolver extends AbstractComponent implements PlaceholderResolver {

    public static final String RESOLVER_SCHEME = "crypt";

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getScheme() {
        return RESOLVER_SCHEME;
    }

    @Override
    public String resolve(FabricService fabricService, Map<String, Map<String, String>> configs, String pid, String key, String value) {
        return getEncryptor(fabricService).decrypt(value.substring(RESOLVER_SCHEME.length() + 1));
    }

    private PBEStringEncryptor getEncryptor(FabricService fabricService) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm(getAlgorithm(fabricService));
        encryptor.setPassword(getPassword(fabricService));
        return encryptor;
    }

    private String getAlgorithm(FabricService fabricService) {
        try {
            return getStringData(fabricService.adapt(CuratorFramework.class), AUTHENTICATION_CRYPT_ALGORITHM.getPath());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    private String getPassword(FabricService fabricService) {
        try {
            return getStringData(fabricService.adapt(CuratorFramework.class), AUTHENTICATION_CRYPT_PASSWORD.getPath());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }
}
