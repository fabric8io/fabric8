/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.maven.util.decrypt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.DefaultSettingsProblem;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.building.SettingsProblem.Severity;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

/**
 * Default settings decrypter configured to decrypt Maven encrypted passwords.
 *
 * Inspired by DefaultSettingsDecrypter from Sonatype
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 * @author Benjamin Bentmann
 */
public class MavenSettingsDecrypter implements SettingsDecrypter {

    private SecDispatcher securityDispatcher;

    public MavenSettingsDecrypter(String securitySettings) {
        this.securityDispatcher = new MavenSecurityDispatcher(securitySettings);
    }

    public SettingsDecryptionResult decrypt(SettingsDecryptionRequest request) {
        List<SettingsProblem> problems = new ArrayList<SettingsProblem>();

        List<Server> servers = new ArrayList<Server>();

        for (Server server : request.getServers()) {
            server = server.clone();
            servers.add(server);

            try {
                server.setPassword(decrypt(server.getPassword()));
            } catch (SecDispatcherException e) {
                problems.add(new DefaultSettingsProblem("Failed to decrypt password for server " + server.getId()
                        + ": " + e.getMessage(), Severity.ERROR, "server: " + server.getId(), -1, -1, e));
            }

            try {
                server.setPassphrase(decrypt(server.getPassphrase()));
            } catch (SecDispatcherException e) {
                problems.add(new DefaultSettingsProblem("Failed to decrypt passphrase for server " + server.getId()
                        + ": " + e.getMessage(), Severity.ERROR, "server: " + server.getId(), -1, -1, e));
            }
        }

        List<Proxy> proxies = new ArrayList<Proxy>();

        for (Proxy proxy : request.getProxies()) {
            proxy = proxy.clone();
            proxies.add(proxy);

            try {
                proxy.setPassword(decrypt(proxy.getPassword()));
            } catch (SecDispatcherException e) {
                problems.add(new DefaultSettingsProblem("Failed to decrypt password for proxy " + proxy.getId()
                        + ": " + e.getMessage(), Severity.ERROR, "proxy: " + proxy.getId(), -1, -1, e));
            }
        }

        return new MavenSettingsDecryptionResult(servers, proxies, problems);
    }

    private String decrypt(String str) throws SecDispatcherException {
        return (str == null) ? null : securityDispatcher.decrypt(str);
    }

}
