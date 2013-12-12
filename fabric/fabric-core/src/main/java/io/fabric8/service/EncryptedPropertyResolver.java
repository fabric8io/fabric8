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

import java.util.Map;

import static io.fabric8.zookeeper.ZkPath.AUTHENTICATION_CRYPT_ALGORITHM;
import static io.fabric8.zookeeper.ZkPath.AUTHENTICATION_CRYPT_PASSWORD;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.FabricException;
import io.fabric8.api.PlaceholderResolver;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
@ThreadSafe
@Component(name = "io.fabric8.placholder.resolver.crypt", description = "Fabric Encrypted Property Placeholder Resolver")
@Service(PlaceholderResolver.class)
public final class EncryptedPropertyResolver extends AbstractComponent implements PlaceholderResolver {

    private static final String CRYPT_SCHEME = "crypt";

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

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
        return CRYPT_SCHEME;
    }

    @Override
    public String resolve(Map<String, Map<String, String>> configs, String pid, String key, String value) {
        assertValid();
        return getEncryptor().decrypt(value.substring(CRYPT_SCHEME.length() + 1));
    }

    private PBEStringEncryptor getEncryptor() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm(getAlgorithm());
        encryptor.setPassword(getPassword());
        return encryptor;
    }

    private String getAlgorithm() {
        try {
            return getStringData(curator.get(), AUTHENTICATION_CRYPT_ALGORITHM.getPath());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    private String getPassword() {
        try {
            return getStringData(curator.get(), AUTHENTICATION_CRYPT_PASSWORD.getPath());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }
}
