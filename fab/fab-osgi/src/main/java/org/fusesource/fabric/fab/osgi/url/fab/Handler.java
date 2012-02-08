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

package org.fusesource.fabric.fab.osgi.url.fab;

import org.fusesource.fabric.fab.osgi.internal.Configuration;
import org.fusesource.fabric.fab.osgi.internal.FabConnection;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Implements the "fab:" protocol
 */
public class Handler extends URLStreamHandler {

    private BundleContext bundleContext;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // TODO
        return super.clone();

    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        Configuration config = Configuration.newInstance();
        return new FabConnection(url, config, bundleContext);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}