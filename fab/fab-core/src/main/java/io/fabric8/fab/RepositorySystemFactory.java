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
package io.fabric8.fab;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.impl.internal.Slf4jLogger;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.log.Logger;

public class RepositorySystemFactory extends DefaultServiceLocator {

    public static RepositorySystem newRepositorySystem() throws Exception
    {
        return new RepositorySystemFactory().getService(RepositorySystem.class);
    }

    public RepositorySystemFactory() {
        super();
        setService(Logger.class, LogAdapter.class);
        setService(WagonProvider.class, StaticWagonProvider.class);
        setService(VersionResolver.class, DefaultVersionResolver.class);
        setService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        setService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        setService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
    }

    public static class LogAdapter extends Slf4jLogger {
        public LogAdapter() {
            super(LoggerFactory.getLogger("org.apache.karaf.pomegranate.Aether"));
        }
    }

    public static class StaticWagonProvider implements WagonProvider {
        public Wagon lookup(String roleHint) throws Exception {
            if ("file".equals(roleHint)) {
                return new FileWagon();
            }
            if ("http".equals(roleHint)) {
                return new LightweightHttpWagon();
            }
            if ("https".equals(roleHint)) {
                return new LightweightHttpsWagon();
            }
            return null;
        }
        public void release(Wagon wagon) {
        }
    }

}
