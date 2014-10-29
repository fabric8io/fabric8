package io.fabric8.gateway.http;

import java.util.HashMap;

public class HTTPGatewayConfig extends HashMap<String, String> {

    private static final long serialVersionUID = -7563354951392959816L;
    /** The host name used when listening for HTTP traffic" */
    public final static String HOST = "HOST";
    /** Port number to listen on for HTTP requests") */
    public final static String PORT = "PORT";
    /** If enabled then performing a HTTP GET on the path '/' will return a JSON representation of the gateway mappings */
    public final static String ENABLE_INDEX = "ENABLE_INDEX";
    
    public int getPort() {
        return Integer.parseInt(get(PORT));
    }
    
    public String getHost() {
        return get(HOST);
    }
    
    public boolean isIndexEnabled() {
        return Boolean.parseBoolean(get(ENABLE_INDEX));
    }
}
