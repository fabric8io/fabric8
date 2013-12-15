package io.fabric8.commands.support;


import org.apache.curator.framework.CuratorFramework;
import org.jledit.ContentManager;

import java.io.IOException;
import java.nio.charset.Charset;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

public class ZookeeperContentManager implements ContentManager {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final CuratorFramework curator;

    public ZookeeperContentManager(CuratorFramework curator) {
        this.curator = curator;
    }

    /**
     * Loads content from the specified location.
     *
     * @param location
     * @return
     */
    @Override
    public String load(String location) throws IOException {
        try {
            String data = getStringData(curator, location);
            return data != null ? data : "";
        } catch (Exception e) {
            throw new IOException("Failed to read data from zookeeper.", e);
        }
    }

    /**
     * Saves content to the specified location.
     *
     * @param content
     * @param location
     * @return
     */
    @Override
    public boolean save(String content, String location) {
        try {
            setData(curator, location, content);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Saves the {@link String} content to the specified location using the specified {@link java.nio.charset.Charset}.
     *
     * @param content
     * @param charset
     * @param location
     * @return
     */
    @Override
    public boolean save(String content, Charset charset, String location) {
        return save(content, location);
    }

    /**
     * Detect the Charset of the content in the specified location.
     *
     * @param location
     * @return
     */
    @Override
    public Charset detectCharset(String location) {
        return UTF_8;
    }
}
