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
package io.fabric8.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import io.fabric8.zookeeper.internal.SimplePathTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

/**
 * Set of paths which are used by fon.
 */
public enum ZkPath {


    // config nodes
    CONFIGS                        ("/fabric/configs"),
    CONFIGS_CONTAINERS             ("/fabric/configs/containers"),
    CONFIG_CONTAINER               ("/fabric/configs/containers/{container}"),
    CONFIG_DEFAULT_VERSION         ("/fabric/configs/default-version"),
    CONFIG_VERSIONS                ("/fabric/configs/versions"),
    CONFIG_VERSION                 ("/fabric/configs/versions/{version}"),
    CONFIG_VERSIONS_PROFILES       ("/fabric/configs/versions/{version}/profiles"),
    CONFIG_VERSIONS_PROFILE        ("/fabric/configs/versions/{version}/profiles/{profile}"),
    CONFIG_VERSIONS_CONTAINERS     ("/fabric/configs/versions/{version}/containers"),
    CONFIG_VERSIONS_CONTAINER      ("/fabric/configs/versions/{version}/containers/{container}"),
	CONFIG_ENSEMBLES               ("/fabric/configs/ensemble"),
	CONFIG_ENSEMBLE_URL            ("/fabric/configs/ensemble/url"),
	CONFIG_ENSEMBLE_PASSWORD       ("/fabric/configs/ensemble/password"),
	CONFIG_ENSEMBLE                ("/fabric/configs/ensemble/{id}"),
	CONFIG_ENSEMBLE_GENERAL        ("/fabric/configs/ensemble/general"),
	CONFIG_ENSEMBLE_PROFILES       ("/fabric/configs/ensemble/profiles"),
	CONFIG_ENSEMBLE_PROFILE        ("/fabric/configs/ensemble/profiles/{profile}"),



    BOOTSTRAP                      ("/fabric/registry/bootstrap"),
    MAVEN_PROXY                    ("/fabric/registry/maven/proxy/{type}"),
	GIT                            ("/fabric/registry/clusters/git"),
	AUTO_SCALE                     ("/fabric/registry/clusters/autoscale"),
	OPENSHIFT                      ("/fabric/registry/clusters/openshift"),
    WEBAPPS_CLUSTERS               ("/fabric/registry/clusters/webapps"),
    WEBAPPS_CLUSTER                ("/fabric/registry/clusters/webapps/{group}"),
    SERVLETS_CLUSTER               ("/fabric/registry/clusters/servlets/{group}"),
    MQ_CLUSTERS                    ("/fabric/registry/clusters/fusemq"),
    MQ_CLUSTER                     ("/fabric/registry/clusters/fusemq/{group}"),
    TASK                           ("/fabric/registry/clusters/task/{task}"),
    TAKS_MEMBERS                   ("/fabric/registry/clusters/task/{task}/0{member}"),
    TASK_MEMBER_PARTITIONS         ("/fabric/registry/containers/task/{container}/{task}"),

    // Agent nodes
    CONTAINERS                     ("/fabric/registry/containers/config"),
    CONTAINER                      ("/fabric/registry/containers/config/{container}"),
    CONTAINER_DOMAINS              ("/fabric/registry/containers/domains/{container}"),
    CONTAINER_DOMAIN               ("/fabric/registry/containers/domains/{container}/{domain}"),
    CONTAINER_ALIVE                ("/fabric/registry/containers/alive/{container}"),
    CONTAINER_PROCESS_ID           ("/fabric/registry/containers/status/{container}/pid"),
    CONTAINER_PROVISION            ("/fabric/registry/containers/provision/{container}"),
    CONTAINER_PROVISION_LIST       ("/fabric/registry/containers/provision/{container}/list"),
    CONTAINER_PROVISION_CHECKSUMS  ("/fabric/registry/containers/provision/{container}/checksums"),
    CONTAINER_PROVISION_RESULT     ("/fabric/registry/containers/provision/{container}/result"),
    CONTAINER_PROVISION_EXCEPTION  ("/fabric/registry/containers/provision/{container}/exception"),
    CONTAINER_EXTENDER             ("/fabric/registry/containers/provision/{container}/extender/{extender}"),
    CONTAINER_EXTENDER_BUNDLE      ("/fabric/registry/containers/provision/{container}/extender/{extender}/bundle/{bundle}"),
    CONTAINER_EXTENDER_STATUS      ("/fabric/registry/containers/provision/{container}/extender/{extender}/status"),
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
    CONTAINER_BINDADDRESS          ("/fabric/registry/containers/config/{container}/bindaddress"),
    CONTAINER_PARENT               ("/fabric/registry/containers/config/{container}/parent"),
    CONTAINER_JMX                  ("/fabric/registry/containers/config/{container}/jmx"),
    CONTAINER_JOLOKIA              ("/fabric/registry/containers/config/{container}/jolokia"),
    CONTAINER_SSH                  ("/fabric/registry/containers/config/{container}/ssh"),
    CONTAINER_HTTP                 ("/fabric/registry/containers/config/{container}/http"),
    CONTAINER_LOCATION             ("/fabric/registry/containers/config/{container}/loc"),
    CONTAINER_METADATA             ("/fabric/registry/containers/config/{container}/metadata"),
    CONTAINER_GEOLOCATION          ("/fabric/registry/containers/config/{container}/geoloc"),
    CONTAINER_OPENSHIFT            ("/fabric/registry/containers/config/{container}/openshift"),
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
    POLICIES                       ("/fabric/registry/policies/{policy}"),

    WEBAPPS                        ("/fabric/registry/webapps"),
    WEBAPPS_CONTAINER              ("/fabric/registry/clusters/webapps/{name}/{version}/{container}"),

    API_REST_ENDPOINTS             ("/fabric/registry/clusters/apis/rest/{name}/{version}/{container}"),
    API_WS_ENDPOINTS               ("/fabric/registry/clusters/apis/ws/{name}/{version}/{container}"),

    LOCK                           ("{path}/0"),
    
    PORTS                          ("/fabric/registry/ports"),
    PORTS_LOCK                     ("/fabric/registry/ports/lock"),
    PORTS_CONTAINER                ("/fabric/registry/ports/containers/{container}"),
    PORTS_CONTAINER_PID            ("/fabric/registry/ports/containers/{container}/{pid}"),
    PORTS_CONTAINER_PID_KEY        ("/fabric/registry/ports/containers/{container}/{pid}/{key}"),
    PORTS_IP                       ("/fabric/registry/ports/ip/{address}"),

    AUTHENTICATION_CRYPT_ALGORITHM ("/fabric/authentication/crypt/algorithm"),
    AUTHENTICATION_CRYPT_PASSWORD  ("/fabric/authentication/crypt/password");



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
    public static byte[] loadURL(CuratorFramework curator, String url) throws Exception {
        URI uri = new URI(url);
        String ref = uri.getFragment();
        String path = uri.getSchemeSpecificPart();
        path = path.trim();
        if( !path.startsWith("/") ) {
            path = ZkPath.CONTAINER.getPath(path);
        }

        byte rc [] = curator.getData().forPath(path);
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
                String textValue = node.asText();
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

    public static void createContainerPaths(CuratorFramework curator, String container, String version, String profiles) throws Exception {
        boolean versionProvided = version != null;

        String v = version;
        //Try to find the version to use
        if (v == null && exists(curator, CONFIG_CONTAINER.getPath(container)) != null) {
            //Try to acquire the version from the registry path /fabric/configs/containers/{container}
            v = getStringData(curator, CONFIG_CONTAINER.getPath(container));
        }  else if (exists(curator, CONFIG_DEFAULT_VERSION.getPath()) != null) {
            //If version is still null, try the default version.
            v = getStringData(curator, CONFIG_DEFAULT_VERSION.getPath());
        } else {
            //Else assume version 1.0.
            v = ZkDefs.DEFAULT_VERSION;
        }

        //Set the version
        if (exists(curator, ZkPath.CONFIG_CONTAINER.getPath(container)) == null || versionProvided) {
            setData(curator, ZkPath.CONFIG_CONTAINER.getPath(container), v);
        }

        //Set the profiles
        if (profiles != null && !profiles.isEmpty() && exists(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(v, container)) == null) {
            setData(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(v, container), profiles);
        }
    }
}
