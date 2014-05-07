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
package io.fabric8.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import io.fabric8.api.FabricService;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.Validatable;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.api.scr.ValidationSupport;

import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerService;

@ThreadSafe
@Component(name = "io.fabric8.mvel.urlhandler", label = "Fabric8 MVEL URL Handler", immediate = true, metatype = false)
@Service(URLStreamHandlerService.class)
@Properties({
        @Property(name = "url.handler.protocol", value = MvelUrlHandler.SCHEME)
})
public final class MvelUrlHandler extends AbstractURLStreamHandlerService implements Validatable {

	protected static final String SCHEME = "mvel";
    private static final String SYNTAX = SCHEME + ":<resource name>";

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    
    private final ValidationSupport active = new ValidationSupport();

    @Activate
    void activate() {
    	active.setValid();
    }

    @Deactivate
    void deactivate() {
    	active.setInvalid();
    }

    @Override
    public boolean isValid() {
        return active.isValid();
    }

    @Override
    public void assertValid() {
        active.assertValid();
    }

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        assertValid();
        return new Connection(url);
    }

    private class Connection extends URLConnection {

        public Connection(URL url) throws MalformedURLException {
            super(url);
            if (url.getPath() == null || url.getPath().trim().length() == 0) {
                throw new MalformedURLException("Path can not be null or empty. Syntax: " + SYNTAX);
            }
            if ((url.getHost() != null && url.getHost().length() > 0) || url.getPort() != -1) {
                throw new MalformedURLException("Unsupported host/port in " + SCHEME + " url");
            }
            if (url.getQuery() != null && url.getQuery().length() > 0) {
                throw new MalformedURLException("Unsupported query in " + SCHEME + " url");
            }
        }

        @Override
        public void connect() throws IOException {
            assertValid();
        }

        @Override
        public InputStream getInputStream() throws IOException {
        	assertValid();
        	String path = url.getPath();
            URL url = new URL(path);
            CompiledTemplate compiledTemplate = TemplateCompiler.compileTemplate(url.openStream());
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("profile", fabricService.get().getCurrentContainer().getOverlayProfile());
            data.put("runtime", runtimeProperties.get());
            String content = TemplateRuntime.execute(compiledTemplate, data).toString();
            return new ByteArrayInputStream(content.getBytes());
        }
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }
    
    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }
}
