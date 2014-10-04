/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.rest;

import io.fabric8.common.util.Files;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.fabric8.rest.ProfileResource.guessMediaType;

/**
 * Represents the fabric registry (access to ZooKeeper).
 */
@Produces("application/json")
public class RegistryResource extends ResourceSupport {
    public static final String FABRIC_ZK_PREFIX = "/fabric/registry/";
    public static final String ROOT_ZK_PREFIX = "/";

    private static final Logger LOG = LoggerFactory.getLogger(RegistryResource.class);
    private final String resourcePathPrefix;
    private final String zkPrefix;

    public RegistryResource(ResourceSupport parentResource, String resourcePathPrefix, String zkPrefix) {
        super(parentResource, resourcePathPrefix);
        this.resourcePathPrefix = resourcePathPrefix;
        this.zkPrefix = zkPrefix;
    }

    @GET
    public Map<String, String> links() {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("data", getLink("data"));
        links.put("children", getLink("children"));
        if (zkPrefix.length() > 0 && !"/".equals(zkPrefix)) {
            String baseUri = getBaseUri();
            int idx = baseUri.lastIndexOf('/');
            if (idx > 0) {
                String parentLink = baseUri.substring(0, idx);
                links.put("parent", parentLink);
            }
        }
        return links;
    }

    @GET
    @Path("children")
    public Map<String, String> list() throws Exception {
        String zkPath = toZooKeeperPath("");
        List<String> fileNames = ZooKeeperUtils.getChildrenSafe(getCurator(), zkPath);
        return mapToLinks(fileNames, "child/");
    }

    @Path("child/{path}")
    public RegistryResource child(@PathParam("path") String path) {
        return new RegistryResource(this, "child/" + path + "/", toZooKeeperPath(path) + "/");
    }

    @GET
    @Path("data")
    public Response get() throws Exception {
        CuratorFramework curator = getCurator();
        if (curator != null) {
            String zkPath = toZooKeeperPath("");
            byte[] bytes = ZooKeeperUtils.getData(curator, zkPath);
            if (bytes == null) {
                return Response.status(Response.Status.NOT_FOUND).
                        entity("No such ZooKeeper entry for path: " + zkPath).build();
            } else {
                String mediaType = guessMediaTypeWithJsonDefault(zkPath, bytes);
                return Response.ok(bytes, mediaType).build();
            }
        } else {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).
                    entity("No ZooKeeper connection").build();
        }
    }

    protected static String guessMediaTypeWithJsonDefault(String path, byte[] bytes) {
        String mediaType = guessMediaType(path);
        if (mediaType.equals("text/plain")) {
            String fileExtension = Files.getFileExtension(path);
            if (fileExtension == null) {
                // if we start and end with {} or [] that we're json
                String trimmedText = new String(bytes).trim();
                if (trimmedText.startsWith("[") && trimmedText.endsWith("]")
                        || trimmedText.startsWith("{") && trimmedText.endsWith("}")) {
                    mediaType = "application/json";
                }
            }
        }
        return mediaType;
    }

    protected String toZooKeeperPath(String path) {
        if (path != null) {
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
        }
        if (path == null || path.length() == 0 || path.equals("/")) {
            if (zkPrefix.endsWith("/") && zkPrefix.length() > 1) {
                return zkPrefix.substring(0, zkPrefix.length() - 1);
            } else {
                return zkPrefix;
            }
        }
        return zkPrefix + path;
    }
}
