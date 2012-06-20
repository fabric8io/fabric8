/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.fabric.service.jclouds.commands;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import com.google.common.base.Strings;
import com.sun.jersey.core.util.StringIgnoreCaseKeyComparator;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import org.fusesource.fabric.service.jclouds.internal.CloudUtils;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "cloud-provider-add", scope = "fabric", description = "Registers a cloud provider with the fabric.")
public class CloudProviderAdd extends FabricCommand {

    @Argument(index = 0, name = "provider", required = true, description = "The cloud provider name")
    private String provider;
    @Argument(index = 1, name = "identity", required = false, description = "The identity used to access the cloud provider")
    private String identity;
    @Argument(index = 2, name = "credential", required = true, description = "The credential used to access the cloud provider")
    private String credential;

    @Option(name = "--async-registration", required = false, description = "Do not wait for the provider registration.")
    private Boolean registerAsync = false;

    @Option(name = "--owner", required = false, description = "EC2 AMI owner")
    private String owner;

    @Option(name = "--option", required = false, multiValued = true, description = "Provider specific properties. Example: --option jclouds.regions=us-east-1")
    private String[] options;

    @Override
    protected Object doExecute() throws Exception {
        Map<String, String> props = CloudUtils.parseProviderOptions(options);
        if (options != null && options.length > 1) {
            for (String option : options) {
                if (option.contains("=")) {
                    String key = option.substring(0,option.indexOf("="));
                    String value = option.substring(option.lastIndexOf("=") + 1);
                    props.put(key, value);
                }
            }
        }

        if (!Strings.isNullOrEmpty(owner)) {
            props.put("owner", owner);
        }

        CloudUtils.registerProvider(getZooKeeper(), configurationAdmin, provider, identity, credential, props);
        if (!registerAsync) {
            System.out.println("Waiting for " + provider + " service to initialize.");
            CloudUtils.waitForComputeService(bundleContext, provider);
        }
        return null;
    }
}
