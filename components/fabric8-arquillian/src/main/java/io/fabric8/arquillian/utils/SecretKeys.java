/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.arquillian.utils;

import io.fabric8.utils.Base64Encoder;

import java.security.KeyPairGenerator;
import java.util.HashMap;
import java.util.Map;

public enum SecretKeys {

    GPG_KEY("fabric8.io/secret-gpg-key") {
        @Override
        public String generate() {
            return "";
        }
    },
    SSH_KEY("fabric8.io/secret-ssh-key") {
        @Override
        public String generate() {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(512);
                byte[] publicKey = keyGen.genKeyPair().getPublic().getEncoded();
                return new String(Base64Encoder.encode(publicKey));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    },
    SSH_PUBLIC_KEY("fabric8.io/secret-ssh-public-key") {
        @Override
        public String generate() {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(512);
                byte[] publicKey = keyGen.genKeyPair().getPublic().getEncoded();
                return new String(Base64Encoder.encode(publicKey));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    };

    private static final Map<String, SecretKeys> map = new HashMap();

    static {
        for (SecretKeys s :SecretKeys.values()) {
            map.put(s.value, s);
        }
    }

    private final String value;

    SecretKeys(String value) {
        this.value = value;
    }

    public abstract String generate();

    public static boolean isSecretKey(String key) {
        return map.containsKey(key);
    }

    public static SecretKeys fromValue(String v) {
        return map.get(v);
    }
}
