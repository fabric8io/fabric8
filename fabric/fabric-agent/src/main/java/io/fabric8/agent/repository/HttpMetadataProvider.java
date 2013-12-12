package io.fabric8.agent.repository;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import io.fabric8.utils.json.JsonReader;

/**
 */
public class HttpMetadataProvider implements MetadataProvider {

    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String GZIP = "gzip";

    private final String url;
    private long lastModified;
    private Map<String, Map<String, String>> metadatas;

    public HttpMetadataProvider(String url) {
        this.url = url;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public Map<String, Map<String, String>> getMetadatas() {
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            if (lastModified > 0) {
                con.setIfModifiedSince(lastModified);
            }
            con.setRequestProperty(HEADER_ACCEPT_ENCODING, GZIP);
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                lastModified = con.getLastModified();
                InputStream is = con.getInputStream();
                if (GZIP.equals(con.getHeaderField(HEADER_CONTENT_ENCODING))) {
                    is = new GZIPInputStream(is);
                }
                metadatas = verify(JsonReader.read(is));
            } else if (con.getResponseCode() != HttpURLConnection.HTTP_NOT_MODIFIED) {
                throw new IOException("Unexpected http response: "
                        + con.getResponseCode() + " " + con.getResponseMessage());
            }
            return metadatas;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Map<String, String>> verify(Object value) {
        Map<?,?> obj = Map.class.cast(value);
        for (Map.Entry<?,?> entry : obj.entrySet()) {
            String.class.cast(entry.getKey());
            Map<?,?> child = Map.class.cast(entry.getValue());
            for (Map.Entry<?,?> ce : child.entrySet()) {
                String.class.cast(ce.getKey());
                String.class.cast(ce.getValue());
            }
        }
        return (Map<String, Map<String, String>>) obj;
    }

}
