/*
 * #%L
 * JBossOSGi SPI
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.fusesource.test.fabric.runtime.wildfly;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test JAAS integration for FabricRealm.
 *
 * @author thomas.diesler@jbos.com
 * @since 11-Dec-2013
 */
@RunWith(Arquillian.class)
public class ZookeeperLoginModuleTest  {

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "fabric-realm.jar");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addIdentityCapability(archive.getName(), "1.0.0");
                builder.addManifestHeader("Dependencies", "org.jboss.modules,org.fusesource.fabric.jaas");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testLoginModuleLoading() throws Exception {
        ModuleLoader moduleLoader = Module.getCallerModuleLoader();
        Module module = moduleLoader.loadModule(ModuleIdentifier.fromString("org.fusesource.fabric.jaas"));
        module.getClassLoader().loadClass("org.fusesource.fabric.jaas.ZookeeperLoginModule").newInstance();
    }

    @Test
    public void testUsernamePasswordLogin() throws Exception {
        Subject subject = new Subject();
        LoginContext loginContext = new LoginContext("fabric-domain", subject, new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (int i = 0; i < callbacks.length; i++) {
                    if (callbacks[i] instanceof NameCallback) {
                        ((NameCallback) callbacks[i]).setName("admin");
                    } else if (callbacks[i] instanceof PasswordCallback) {
                        ((PasswordCallback) callbacks[i]).setPassword("admin".toCharArray());
                    } else {
                        throw new UnsupportedCallbackException(callbacks[i]);
                    }
                }
            }
        });
        loginContext.login();
        Principal user = null;
        for (Principal p : subject.getPrincipals()) {
            if ("admin".equals(p.getName())) {
                user = p;
            }
        }
        Assert.assertNotNull("UserPricipal found", user);
    }
}
