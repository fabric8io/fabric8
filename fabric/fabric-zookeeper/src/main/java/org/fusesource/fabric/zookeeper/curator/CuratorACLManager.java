
package org.fusesource.fabric.zookeeper.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.fusesource.fabric.zookeeper.ACLManager;
import org.linkedin.util.io.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CuratorACLManager implements ACLManager, ACLProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CuratorACLManager.class);
    private final ConcurrentMap<String, String> acls = new ConcurrentHashMap<String, String>();

    private CuratorFramework curator;

    public CuratorACLManager() {
        acls.put("/", "world:anyone:acdrw");
        acls.put("/fabric", "auth::acdrw,world:anyone:");
    }

    @Override
    public List<ACL> getDefaultAcl() {
        return getAclForPath("/");
    }

    @Override
    public List<ACL> getAclForPath(String path) {
        String acl = findNodeAcls(adjustPath(path));
        if (acl == null) {
            throw new IllegalStateException("Could not find matching ACLs for " + path);
        }
        return parseACLs(acl);
    }

    @Override
    public void registarAcl(String path, String acl) {
        acls.put(path, acl);
    }

    @Override
    public void unregisterAcl(String path) {
        acls.remove(path);
    }

    @Override
    public void fixAcl(String path, boolean recursive) throws Exception {
        doFixACLs(path, recursive);
    }

    private void doFixACLs(String path, boolean recursive) throws Exception {
        List<ACL> aclList = getAclForPath(path);
        curator.setACL().withACL(aclList).forPath(path);
        if (recursive) {
            for (String child : curator.getChildren().forPath(path)) {
                doFixACLs(path.equals("/") ? "/" + child : path + "/" + child, recursive);
            }
        }
    }

    /**
     * Returns the ACL string for the specified path.
     *
     * @param path
     * @return
     */
    private String findNodeAcls(String path) {
        String longestPath = "";
        for (String acl : acls.keySet()) {
            if (acl.length() > longestPath.length() && path.startsWith(acl)) {
                longestPath = acl;
            }
        }
        return acls.get(longestPath);
    }

    /**
     * Normalizes the specified path, by removing trailing slashes and adding leading ones if needed.
     *
     * @param path
     * @return
     */
    private String adjustPath(String path) {
        path = PathUtils.removeTrailingSlash(path);
        path = PathUtils.addLeadingSlash(path);
        return path;
    }

    /**
     * Parses a {@link String} representation of the {@link ACL} list.
     *
     * @param aclString
     * @return
     */
    private List<ACL> parseACLs(String aclString) {
        List<ACL> acl;
        String acls[] = aclString.split(",");
        acl = new ArrayList<ACL>();
        for (String a : acls) {
            int firstColon = a.indexOf(':');
            int lastColon = a.lastIndexOf(':');
            if (firstColon == -1 || lastColon == -1 || firstColon == lastColon) {
                LOGGER.warn(a + " does not have the form scheme:id:perm");
                continue;
            }
            ACL newAcl = new ACL();
            newAcl.setId(new Id(a.substring(0, firstColon), a.substring(
                    firstColon + 1, lastColon)));
            newAcl.setPerms(getPermFromString(a.substring(lastColon + 1)));
            acl.add(newAcl);
        }
        return acl;
    }

    /**
     * Returns the int value of the permission {@link String}.
     *
     * @param permString
     * @return
     */
    private int getPermFromString(String permString) {
        int perm = 0;
        for (int i = 0; i < permString.length(); i++) {
            switch (permString.charAt(i)) {
                case 'r':
                    perm |= ZooDefs.Perms.READ;
                    break;
                case 'w':
                    perm |= ZooDefs.Perms.WRITE;
                    break;
                case 'c':
                    perm |= ZooDefs.Perms.CREATE;
                    break;
                case 'd':
                    perm |= ZooDefs.Perms.DELETE;
                    break;
                case 'a':
                    perm |= ZooDefs.Perms.ADMIN;
                    break;
                default:
                    LOGGER.warn("Unknown perm type: " + permString.charAt(i));
            }
        }
        return perm;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }
}