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
package org.fusesource.fabric.zookeeper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import org.apache.zookeeper.KeeperException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.fabric.zookeeper.internal.SimplePathTemplate;
import org.linkedin.zookeeper.client.IZKClient;

/**
 * Set of paths which are used by fon.
 */
public enum ZkPath {


    // config nodes
    CONFIGS_CONTAINERS      ("/fabric/configs/containers/"),
    CONFIG_CONTAINER        ("/fabric/configs/containers/{container}"),
    CONFIG_DEFAULT_VERSION  ("/fabric/configs/default-version"),
    CONFIG_VERSIONS         ("/fabric/configs/versions"),
    CONFIG_VERSION          ("/fabric/configs/versions/{version}"),
    CONFIG_VERSIONS_PROFILES("/fabric/configs/versions/{version}/profiles"),
    CONFIG_VERSIONS_PROFILE ("/fabric/configs/versions/{version}/profiles/{profile}"),
    CONFIG_VERSIONS_CONTAINER("/fabric/configs/versions/{version}/containers/{container}"),
    CONFIGS_MAVEN_REPO      ("/fabric/configs/maven/repository"),
    CONFIGS_MAVEN_PROXY      ("/fabric/configs/maven/proxy"),

    // Agent nodes
    CONTAINERS                     ("/fabric/registry/containers/config"),
    CONTAINER                      ("/fabric/registry/containers/config/{container}"),
    CONTAINER_DOMAINS              ("/fabric/registry/containers/domains/{container}"),
    CONTAINER_DOMAIN               ("/fabric/registry/containers/domains/{container}/{domain}"),
    CONTAINER_ALIVE                ("/fabric/registry/containers/alive/{container}"),
    CONTAINER_PROVISION            ("/fabric/registry/containers/provision/{container}"),
    CONTAINER_PROVISION_RESULT     ("/fabric/registry/containers/provision/{container}/result"),
    CONTAINER_PROVISION_EXCEPTION  ("/fabric/registry/containers/provision/{container}/exception"),
    CONTAINER_IP                   ("/fabric/registry/containers/config/{container}/ip"),
    CONTAINER_PARENT               ("/fabric/registry/containers/config/{container}/parent"),
    CONTAINER_JMX                  ("/fabric/registry/containers/config/{container}/jmx"),
    CONTAINER_SSH                  ("/fabric/registry/containers/config/{container}/ssh"),
    CONTAINER_LOCATION             ("/fabric/registry/containers/config/{container}/loc"),
    CONTAINER_METADATA             ("/fabric/registry/containers/config/{container}/metadata"),
    CONTAINER_MANAGED              ("/fabric/registry/containers/config/{container}/managed");

    /**
     * Path template.
     */
    private SimplePathTemplate path;

    private ZkPath(String path) {
        this.path = new SimplePathTemplate(path);
    }

    /**
     * Gets path.
     *
     * @param args Values of path variables.
     * @return Path
     */
    public String getPath(String ... args) {
        return this.path.bindByPosition(args);
    }


    /**
     * Gets path.
     *
     * @param args Values of path variables.
     * @return Path
     */
    public String getPath(Map<String, String> args) {
        return this.path.bindByName(args);
    }

    /**
     * Loads a zoo keeper URL content using the provided ZooKeeper client.
     */
    static public byte[] loadURL(IZKClient zooKeeper, String url) throws InterruptedException, KeeperException, IOException, URISyntaxException {
        URI uri = new URI(url);
        String ref = uri.getFragment();
        String path = uri.getSchemeSpecificPart();
        path = path.trim();
        if( !path.startsWith("/") ) {
            path = ZkPath.CONTAINER.getPath(path);
        }
        byte rc [] = zooKeeper.getData(path);
        if( ref!=null ) {
            if( path.endsWith(".properties") ) {
                Properties properties = new Properties();
                properties.load(new ByteArrayInputStream(rc));
                String property = properties.getProperty(ref);
                if( property==null ) {
                    throw  new IOException("Property '"+ ref +"' is not set in the properties file.");
                }
                rc = property.getBytes("UTF-8");
            } else if( path.endsWith(".json") ) {
                String[] fields = ref.split("\\.");
                ObjectMapper mapper = new ObjectMapper();
                JsonFactory factory = mapper.getJsonFactory();
                JsonParser jp = factory.createJsonParser(rc);
                JsonNode node = mapper.readTree(jp);
                for(String field: fields) {
                    if(!field.isEmpty()) {
                        if( node.isObject() ) {
                            node = node.get(field);
                        } else if (node.isArray()) {
                            node = node.get(Integer.parseInt(field));
                        } else {
                            throw  new IOException("Path '"+ ref +"' is not set in the json file.");
                        }
                        if( node == null ) {
                            throw  new IOException("Path '"+ ref +"' is not set in the json file.");
                        }
                    }
                }
                if( node.isContainerNode() ) {
                    throw new IOException("Path '"+ ref +"' is not a value in the json file.");
                }
                String textValue = node.getValueAsText();
                rc = textValue.getBytes("UTF-8");
            } else {
                throw new IOException("Do not know how to handle path fragments for path: "+path);
            }
        }
        return rc;
    }

}
