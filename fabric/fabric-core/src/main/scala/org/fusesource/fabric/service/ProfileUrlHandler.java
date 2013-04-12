package org.fusesource.fabric.service;

import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.osgi.service.url.AbstractURLStreamHandlerService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ProfileUrlHandler extends AbstractURLStreamHandlerService {

    private final FabricService fabricService;
    private static final String SYNTAX = "profile:<profile name>/<resource name>";

    public ProfileUrlHandler(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        return new Connection(url);
    }

    public class Connection extends URLConnection {

        public Connection(URL url) throws MalformedURLException {
            super(url);
            if (url.getPath() == null || url.getPath().trim().length() == 0) {
                throw new MalformedURLException("Path can not be null or empty. Syntax: " + SYNTAX);
            }
            if ((url.getHost() != null && url.getHost().length() > 0) || url.getPort() != -1) {
                throw new MalformedURLException("Unsupported host/port in profile url");
            }
            if (url.getQuery() != null && url.getQuery().length() > 0) {
                throw new MalformedURLException("Unsupported query in profile url");
            }
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            String path = url.getPath();
            Profile profile = fabricService.getCurrentContainer().getOverlayProfile();

            if (profile.getFileConfigurations().containsKey(path)) {
                byte[] b = profile.getFileConfigurations().get(path);
                return new ByteArrayInputStream(b);
            } else {
                throw new IllegalArgumentException("Resource " + path + " does not exist in the profile overlay.");
            }
        }
    }
}
