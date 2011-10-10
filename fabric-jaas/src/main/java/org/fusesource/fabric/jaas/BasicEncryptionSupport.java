/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.jaas;

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
                encryption = new BasicEncryption(encOpts);
            }
        }
        return encryption;
    }
}
