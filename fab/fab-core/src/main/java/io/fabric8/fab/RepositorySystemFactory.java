/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.fab;

import java.util.List;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.wagon.WagonProvider;
import org.eclipse.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.internal.impl.Slf4jLoggerFactory;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.spi.log.Logger;

public class RepositorySystemFactory implements ServiceLocator {
    private DefaultServiceLocator delegate;

    public static RepositorySystem newRepositorySystem() throws Exception
    {
        return new RepositorySystemFactory().getService(RepositorySystem.class);
    }

    public RepositorySystemFactory() {
        delegate = new DefaultServiceLocator();
        setService(Logger.class, LogAdapter.class);
        setService(WagonProvider.class, StaticWagonProvider.class);
        setService(VersionResolver.class, DefaultVersionResolver.class);
        setService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        setService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        setService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
    }

    public <T> void setService(Class<T> type, Class<? extends T> impl) {
        delegate.setService(type, impl);
    }

    public <T> T getService(Class<T> type) {
        return delegate.getService(type);
    }

    public <T> List<T> getServices(Class<T> type) {
        return delegate.getServices(type);
    }

    public static class LogAdapter implements Logger {
        private Logger delegate;

        public LogAdapter() {
            delegate = new Slf4jLoggerFactory().getLogger("org.apache.karaf.pomegranate.Aether");
        }

        public boolean isDebugEnabled() {
            return delegate.isDebugEnabled();
        }

        public void debug(String msg) {
            delegate.debug(msg);
        }

        public void debug(String msg, Throwable error) {
            delegate.debug(msg, error);
        }

        public boolean isWarnEnabled() {
            return delegate.isWarnEnabled();
        }

        public void warn(String msg) {
            delegate.warn(msg);
        }

        public void warn(String msg, Throwable error) {
            delegate.warn(msg, error);
        }
    }

    public static class StaticWagonProvider implements WagonProvider {
        public Wagon lookup(String roleHint) throws Exception {
            if ("file".equals(roleHint)) {
                return new FileWagon();
            }
            if ("http".equals(roleHint)) {
                LightweightHttpWagon wagon = new LightweightHttpWagon();
                wagon.setAuthenticator(new LightweightHttpWagonAuthenticator());
                return wagon;
            }
            if ("https".equals(roleHint)) {
                LightweightHttpsWagon wagon = new LightweightHttpsWagon();
                wagon.setAuthenticator(new LightweightHttpWagonAuthenticator());
                return wagon;
            }
            return null;
        }
        public void release(Wagon wagon) {
        }
    }

}
