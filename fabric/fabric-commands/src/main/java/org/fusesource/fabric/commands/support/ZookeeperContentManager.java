package org.fusesource.fabric.commands.support;


import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.jledit.ContentManager;

import java.io.IOException;
import java.nio.charset.Charset;

public class ZookeeperContentManager implements ContentManager {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final IZKClient zookeeper;

    public ZookeeperContentManager(IZKClient zookeeper) {
        this.zookeeper = zookeeper;
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
            String data = zookeeper.getStringData(location);
            return data != null ? data : "";
        } catch (InterruptedException e) {
            throw new IOException("Failed to read data from zookeeper.", e);
        } catch (KeeperException e) {
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
            ZooKeeperUtils.set(zookeeper,location, content);
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
