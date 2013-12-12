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

package io.fabric8.fab.osgi;

import io.fabric8.fab.osgi.internal.FabConnection;
import io.fabric8.fab.osgi.internal.ServiceProvider;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class FabURLHandler extends AbstractURLStreamHandlerService {

    private static String SYNTAX = "fab: fab-jar-uri";
    private static final Logger logger = LoggerFactory.getLogger(FabURLHandler.class);

    private ServiceProvider serviceProvider;

    private FabResolverFactory fabResolverFactory;

    /**
     * Open the connection for the given URL.
     *
     * @param url the url from which to open a connection.
     * @return a connection on the specified URL.
     * @throws java.io.IOException if an error occurs or if the URL is malformed.
     */
    @Override
	public FabConnection openConnection(URL url) throws IOException {
		if (url.getPath() == null || url.getPath().trim().length() == 0) {
			throw new MalformedURLException("Path can not be null or empty. Syntax: " + SYNTAX );
		}
		URL fabJarURL = new URL(url.getPath());

		logger.debug("FAB jar URL is: [" + fabJarURL + "]");

        return new FabConnection(fabJarURL, fabResolverFactory, serviceProvider);
	}

    public void setFabResolverFactory(FabResolverFactory fabResolverFactory) {
        this.fabResolverFactory = fabResolverFactory;
    }

    public void setServiceProvider(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }
}

