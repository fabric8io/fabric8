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
package io.fabric8.osgimetadata;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ExecutionException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

/**
 */
public class MavenOsgiMetadataProviderFactory extends BaseManagedServiceFactory<MavenOsgiMetadataProviderFactory.AliasedProvider> {

    private HttpService httpService;

    public MavenOsgiMetadataProviderFactory(BundleContext context) {
        super(context, "MavenOsgiMetadataProviderFactory");
    }

    public void bindHttpService(HttpService httpService) {
        this.httpService = httpService;
        execute(new Runnable() {
            @Override
            public void run() {
                for (Pair<AliasedProvider, ServiceRegistration> service : getServices().values()) {
                    AliasedProvider provider = service.getFirst();
                    doRegister(provider);
                }
            }
        });
    }

    public void unbindHttpService(HttpService httpService) throws ExecutionException, InterruptedException {
        if (isDestroyed()) {
            return;
        }
        try {
            execute(new Runnable() {
                @Override
                public void run() {
                    for (Pair<AliasedProvider, ServiceRegistration> service : getServices().values()) {
                        AliasedProvider provider = service.getFirst();
                        doUnregister(provider);
                    }
                }
            }).get();
        } finally {
            this.httpService = null;
        }
    }

    private void doRegister(AliasedProvider provider) {
        try {
            this.httpService.registerServlet(provider.getAlias(),
                    new MavenOsgiMetadataServlet(provider),
                    new Hashtable(), null);
        } catch (Exception e) {
            LOGGER.warn("Error registering osgi metadata servlet", e);
        }
    }

    private void doUnregister(AliasedProvider provider) {
        try {
            this.httpService.unregister(provider.getAlias());
        } catch (Exception e) {
            LOGGER.warn("Error unregistering osgi metadata servlet", e);
        }
    }

    @Override
    protected AliasedProvider doCreate(Dictionary<String, ?> properties) throws Exception {
        String alias = getString(properties, "alias");
        String path = getString(properties, "path");
        String dirs = getString(properties, "dirs", "");
        String files = getString(properties, "files", "glob:**/*.jar");
        if (alias == null) {
            throw new IllegalArgumentException("Property alias not specified");
        }
        if (path == null) {
            throw new IllegalArgumentException("Property path not specified");
        }
        AliasedProvider provider = new AliasedProvider(
                "/osgimetadata/" + alias,
                path,
                dirs,
                files);
        if (this.httpService != null) {
            this.httpService.registerServlet(
                    provider.getAlias(),
                    new MavenOsgiMetadataServlet(provider),
                    new Hashtable(),
                    null
            );
        }
        return provider;
    }

    private String getString(Dictionary<String, ?> properties, String name) {
        return getString(properties, name, null);
    }

    private String getString(Dictionary<String, ?> properties, String name, String def) {
        Object obj = properties.get(name);
        if (obj instanceof String) {
            return ((String) obj);
        } else if (obj != null) {
            return obj.toString();
        } else {
            return def;
        }
    }

    @Override
    protected void doDestroy(AliasedProvider provider) throws Exception {
        provider.destroy();
        if (this.httpService != null) {
            this.httpService.unregister(provider.getAlias());
        }
    }

    @Override
    protected String[] getExposedClasses(AliasedProvider provider) {
        return new String[] { AliasedProvider.class.getName() };
    }

    public static class AliasedProvider extends Maven2MetadataProvider {
        private String alias;
        public AliasedProvider(String alias, String root, String dirMatcher, String fileMatcher) throws IOException {
            super(root, dirMatcher, fileMatcher);
            this.alias = alias;
        }
        public String getAlias() {
            return alias;
        }
    }

}
