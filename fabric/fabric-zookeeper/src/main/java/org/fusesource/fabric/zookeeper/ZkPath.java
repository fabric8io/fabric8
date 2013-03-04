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
import java.util.regex.Pattern;

import org.apache.zookeeper.KeeperException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.fabric.zookeeper.internal.SimplePathTemplate;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;

/**
 * Set of paths which are used by fon.
 */
public enum ZkPath {


    // config nodes
    CONFIGS_CONTAINERS             ("/fabric/configs/containers/"),
    CONFIG_CONTAINER               ("/fabric/configs/containers/{container}"),
    CONFIG_DEFAULT_VERSION         ("/fabric/configs/default-version"),
    CONFIG_VERSIONS                ("/fabric/configs/versions"),
    CONFIG_VERSION                 ("/fabric/configs/versions/{version}"),
    CONFIG_VERSIONS_PROFILES       ("/fabric/configs/versions/{version}/profiles"),
    CONFIG_VERSIONS_PROFILE        ("/fabric/configs/versions/{version}/profiles/{profile}"),
    CONFIG_VERSIONS_CONTAINER      ("/fabric/configs/versions/{version}/containers/{container}"),
	CONFIG_ENSEMBLES               ("/fabric/configs/ensemble"),
	CONFIG_ENSEMBLE_URL            ("/fabric/configs/ensemble/url"),
	CONFIG_ENSEMBLE_PASSWORD       ("/fabric/configs/ensemble/password"),
	CONFIG_ENSEMBLE                ("/fabric/configs/ensemble/{id}"),
	CONFIG_ENSEMBLE_GENERAL        ("/fabric/configs/ensemble/general"),
	CONFIG_ENSEMBLE_PROFILES       ("/fabric/configs/ensemble/profiles"),
	CONFIG_ENSEMBLE_PROFILE        ("/fabric/configs/ensemble/profiles/{profile}"),

    MAVEN_PROXY                    ("/fabric/registry/maven/proxy/{type}"),
    // Agent nodes
    CONTAINERS                     ("/fabric/registry/containers/config"),
    CONTAINER                      ("/fabric/registry/containers/config/{container}"),
    CONTAINER_DOMAINS              ("/fabric/registry/containers/domains/{container}"),
    CONTAINER_DOMAIN               ("/fabric/registry/containers/domains/{container}/{domain}"),
    CONTAINER_ALIVE                ("/fabric/registry/containers/alive/{container}"),
    CONTAINER_PROVISION            ("/fabric/registry/containers/provision/{container}"),
    CONTAINER_PROVISION_LIST       ("/fabric/registry/containers/provision/{container}/list"),
    CONTAINER_PROVISION_RESULT     ("/fabric/registry/containers/provision/{container}/result"),
    CONTAINER_PROVISION_EXCEPTION  ("/fabric/registry/containers/provision/{container}/exception"),
    CONTAINER_ENTRY                ("/fabric/registry/containers/config/{container}/{entry}"),
    CONTAINER_PORT_MIN             ("/fabric/registry/containers/config/{container}/minimumport"),
    CONTAINER_PORT_MAX             ("/fabric/registry/containers/config/{container}/maximumport"),
    CONTAINER_IP                   ("/fabric/registry/containers/config/{container}/ip"),
    CONTAINER_RESOLVER             ("/fabric/registry/containers/config/{container}/resolver"),
    CONTAINER_ADDRESS              ("/fabric/registry/containers/config/{container}/{type}"),
    CONTAINER_LOCAL_IP             ("/fabric/registry/containers/config/{container}/localip"),
    CONTAINER_LOCAL_HOSTNAME       ("/fabric/registry/containers/config/{container}/localhostname"),
    CONTAINER_PUBLIC_IP            ("/fabric/registry/containers/config/{container}/publicip"),
    CONTAINER_PUBLIC_HOSTNAME      ("/fabric/registry/containers/config/{container}/publichostname"),
    CONTAINER_MANUAL_IP            ("/fabric/registry/containers/config/{container}/manualip"),
    CONTAINER_PARENT               ("/fabric/registry/containers/config/{container}/parent"),
    CONTAINER_JMX                  ("/fabric/registry/containers/config/{container}/jmx"),
    CONTAINER_SSH                  ("/fabric/registry/containers/config/{container}/ssh"),
    CONTAINER_LOCATION             ("/fabric/registry/containers/config/{container}/loc"),
    CONTAINER_METADATA             ("/fabric/registry/containers/config/{container}/metadata"),
    CONTAINER_GEOLOCATION          ("/fabric/registry/containers/config/{container}/geoloc"),
    CLOUD_CONFIG                   ("/fabric/registry/cloud/config"),
    CLOUD_SERVICE                  ("/fabric/registry/cloud/config/{service}"),
    CLOUD_SERVICE_PROVIDER         ("/fabric/registry/cloud/config/{service}/provider"),
    CLOUD_SERVICE_API              ("/fabric/registry/cloud/config/{service}/api"),
    CLOUD_SERVICE_ENDPOINT         ("/fabric/registry/cloud/config/{service}/endpoint"),
    CLOUD_SERVICE_IDENTITY         ("/fabric/registry/cloud/config/{service}/identity"),
    CLOUD_SERVICE_CREDENTIAL       ("/fabric/registry/cloud/config/{service}/credential"),
    CLOUD_SERVICE_PROPERTY         ("/fabric/registry/cloud/config/{service}/{property}"),
    CLOUD_NODES                    ("/fabric/registry/cloud/nodes"),
    CLOUD_NODE                     ("/fabric/registry/cloud/nodes/{id}"),
    CLOUD_NODE_IDENTITY            ("/fabric/registry/cloud/nodes/{id}/identity"),
    CLOUD_NODE_CREDENTIAL          ("/fabric/registry/cloud/nodes/{id}/credential"),
    POLICIES                       ("/fabric/registry/policies/{policy}");

	private static final Pattern ENSEMBLE_PROFILE_PATTERN = Pattern.compile("fabric-ensemble-[0-9]+|fabric-ensemble-[0-9]+-[0-9]+");

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
    public static byte[] loadURL(IZKClient zooKeeper, String url) throws InterruptedException, KeeperException, IOException, URISyntaxException {
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

	/**
	 * Returns the path of the profile.
	 * @param version
	 * @param id
	 * @return
	 */
	public static String getProfilePath(String version, String id) {
		if (ENSEMBLE_PROFILE_PATTERN.matcher(id).matches()) {
			return ZkPath.CONFIG_ENSEMBLE_PROFILE.getPath(id);
		} else return ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id);
	}

    public static void createContainerPaths(IZKClient zooKeeper, String container, String version, String profiles) throws InterruptedException, KeeperException {
        boolean versionProvided = version != null;

        String v = version;
        //Try to find the version to use
        if (v == null && zooKeeper.exists(CONFIG_CONTAINER.getPath(container)) != null) {
            //Try to acquire the version from the registry path /fabric/configs/containers/{container}
            v = ZooKeeperUtils.get(zooKeeper, CONFIG_CONTAINER.getPath(container));
        }  else if (zooKeeper.exists(CONFIG_DEFAULT_VERSION.getPath()) != null) {
            //If version is still null, try the default version.
            v = ZooKeeperUtils.get(zooKeeper, CONFIG_DEFAULT_VERSION.getPath());
        } else {
            //Else assume version 1.0.
            v = ZkDefs.DEFAULT_VERSION;
        }

        //Set the version
        if (zooKeeper.exists(ZkPath.CONFIG_CONTAINER.getPath(container)) == null || versionProvided) {
            ZooKeeperUtils.set(zooKeeper, ZkPath.CONFIG_CONTAINER.getPath(container), v);
        }

        //Set the profiles
        if (profiles != null && !profiles.isEmpty() && zooKeeper.exists(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(v, container)) == null) {
            ZooKeeperUtils.set(zooKeeper, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(v, container), profiles);
        }
    }
}
