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

import org.apache.karaf.jaas.modules.Encryption;
import org.apache.karaf.jaas.modules.encryption.BasicEncryption;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;

import java.util.HashMap;
import java.util.Map;

public class BasicEncryptionSupport extends EncryptionSupport {

    protected Encryption encryption;

    public BasicEncryptionSupport(Map<String, ?> options) {
        super(options);
    }

    @Override
    public Encryption getEncryption() {
        if (encryption == null) {
            Map<String, String> encOpts = new HashMap<String, String>();
            for (String key : options.keySet()) {
                if (key.startsWith("encryption.")) {
                    encOpts.put(key.substring("encryption.".length()), options.get(key).toString());
                }
            }
            setEncryptionPrefix(encOpts.remove("prefix"));
            setEncryptionSuffix(encOpts.remove("suffix"));
            boolean enabled = Boolean.parseBoolean(encOpts.remove("enabled"));
            if (enabled) {
                encOpts.remove("name");
                encryption = new BasicEncryption(encOpts);
            }
        }
        return encryption;
    }
}
