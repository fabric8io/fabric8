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

import io.fabric8.api.FabricService;
import io.fabric8.common.util.Strings;
import io.fabric8.api.jmx.Links;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;

/**
 * a base class for resource beans
 */
public abstract class ResourceSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(ResourceSupport.class);
    private ResourceSupport parent;

    @Resource
    private MessageContext messageContext;

    private FabricService fabricService;
    private String baseUri;

    public ResourceSupport() {
    }

    public ResourceSupport(ResourceSupport parent) {
        this(parent, "");
    }

    public ResourceSupport(ResourceSupport parent, String pathPrefix) {
        this.parent = parent;
        this.messageContext = parent.messageContext;
        this.fabricService = parent.getFabricService();
        this.baseUri = parent.getBaseUri();
        if (baseUri == null) {
            baseUri = "";
        }
        if (Strings.notEmpty(pathPrefix)) {
            this.baseUri += pathPrefix;
        }
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }


    /**
     * Returns a relative link to the current resource
     */
    protected String getLink(String path) {
        String baseUri = getBaseUri();
        return Links.getLink(path, baseUri);
    }

    public ResourceSupport getParent() {
        return parent;
    }

    protected String getParentBaseUri() {
        if (parent != null) {
            return parent.getBaseUri();
        } else {
            return "";
        }
    }
    protected String getBaseUri() {
        if (baseUri == null) {
            if (messageContext != null) {
                UriInfo uriInfo = messageContext.getUriInfo();
                if (uriInfo != null) {
                    URI baseUriObject = uriInfo.getBaseUri();
                    if (baseUriObject != null) {
                        this.baseUri = baseUriObject.toString();
                    }
                }
            }
            if (baseUri == null) {
                baseUri = "";
            }
        }
        return baseUri;
    }

    /**
     * Returns a map where the key points to the link appending the given path to the current base URI
     */
    protected Map<String, String> mapToLinks(Iterable<String> keys, String path) {
        return Links.mapIdsToLinks(keys, getBaseUri() + path);
    }

    /**
     * Handle a missing FabricService
     */
    protected void noFabricService() {
        LOG.warn("No FabricService available!");
    }
}
