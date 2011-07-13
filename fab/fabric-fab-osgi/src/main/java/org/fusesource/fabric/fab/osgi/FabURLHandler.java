/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.fab.osgi;

import org.fusesource.fabric.fab.osgi.url.internal.Configuration;
import org.fusesource.fabric.fab.osgi.url.internal.FabConnection;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class FabURLHandler extends AbstractURLStreamHandlerService {

	private final Logger logger = LoggerFactory.getLogger(FabURLHandler.class);

	private static String SYNTAX = "fab: fab-jar-uri";

	private URL fabJarURL;

    /**
     * Open the connection for the given URL.
     *
     * @param url the url from which to open a connection.
     * @return a connection on the specified URL.
     * @throws java.io.IOException if an error occurs or if the URL is malformed.
     */
    @Override
	public URLConnection openConnection(URL url) throws IOException {
		if (url.getPath() == null || url.getPath().trim().length() == 0) {
			throw new MalformedURLException("Path can not be null or empty. Syntax: " + SYNTAX );
		}
		fabJarURL = new URL(url.getPath());

		logger.debug("FAB jar URL is: [" + fabJarURL + "]");
        PropertiesPropertyResolver resolver = new PropertiesPropertyResolver(System.getProperties());
        Configuration config = new Configuration(resolver);
        return new FabConnection(url, config);
	}

	public URL getFabJarURL() {
		return fabJarURL;
	}
}

