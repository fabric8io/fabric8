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


package io.fabric8.dosgi.tcp;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import io.fabric8.dosgi.util.IntrospectionSupport;
import io.fabric8.dosgi.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @author David Martin Clavo david(dot)martin(dot)clavo(at)gmail.com (logging improvement modifications)
 */
public class TcpTransportFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TcpTransportFactory.class);

    public TcpTransportServer bind(String location) throws Exception {

        URI uri = new URI(location);
        TcpTransportServer server = createTcpTransportServer(uri);
        if (server == null) return null;

        Map<String, String> options = new HashMap<String, String>(URISupport.parseParameters(uri));
        IntrospectionSupport.setProperties(server, options);
        Map<String, Object> transportOptions = IntrospectionSupport.extractProperties(options, "transport.");
        server.setTransportOption(transportOptions);
        return server;
    }


    public TcpTransport connect(String location) throws Exception {
        URI uri = new URI(location);
        TcpTransport transport = createTransport(uri);
        if (transport == null) return null;

        Map<String, String> options = new HashMap<String, String>(URISupport.parseParameters(uri));
        URI localLocation = getLocalLocation(uri);

        transport.connecting(uri, localLocation);

        Map<String, Object> socketOptions = IntrospectionSupport.extractProperties(options, "socket.");
        transport.setSocketOptions(socketOptions);

        IntrospectionSupport.setProperties(transport, options);
        if (!options.isEmpty()) {
            // Release the transport resource as we are erroring out...
            try {
                transport.stop();
            } catch (Throwable cleanup) {
            }
            throw new IllegalArgumentException("Invalid connect parameters: " + options);
        }
        return transport;
    }

    /**
     * Allows subclasses of TcpTransportFactory to create custom instances of
     * TcpTransportServer.
     */
    protected TcpTransportServer createTcpTransportServer(final URI location) throws IOException, URISyntaxException, Exception {
        if( !location.getScheme().equals("tcp") ) {
            return null;
        }
        return new TcpTransportServer(location);
    }

    /**
     * Allows subclasses of TcpTransportFactory to create custom instances of
     * TcpTransport.
     */
    protected TcpTransport createTransport(URI uri) throws NoSuchAlgorithmException, Exception {
        if( !uri.getScheme().equals("tcp") ) {
            return null;
        }
        TcpTransport transport = new TcpTransport();
        return transport;
    }

    protected URI getLocalLocation(URI location) {
        URI localLocation = null;
        String path = location.getPath();
        // see if the path is a local URI location
        if (path != null && path.length() > 0) {
            int localPortIndex = path.indexOf(':');
            try {
                Integer.parseInt(path.substring(localPortIndex + 1, path.length()));
                String localString = location.getScheme() + ":/" + path;
                localLocation = new URI(localString);
            } catch (Exception e) {
                LOG.warn("path isn't a valid local location for TcpTransport to use", e);
            }
        }
        return localLocation;
    }

    protected String getOption(Map options, String key, String def) {
        String rc = (String) options.remove(key);
        if( rc == null ) {
            rc = def;
        }
        return rc;
    }

}
