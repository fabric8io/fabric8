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
package io.fabric8.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.boot.commands.support.EnsembleCommandSupport;
import io.fabric8.utils.Strings;

import java.util.List;
import java.util.Map;

import static io.fabric8.utils.FabricValidations.validateContainersName;

@Command(name = "ensemble-add", scope = "fabric", description = "Extend the current fabric ensemble by converting the specified containers into ensemble servers", detailedDescription = "classpath:ensembleAdd.txt")
public class EnsembleAdd extends EnsembleCommandSupport {

    @Option(name = "--generate-zookeeper-password", multiValued = false, description = "Flag to enable automatic generation of password")
    private boolean generateZookeeperPassword = false;

    @Option(name = "--new-zookeeper-password", multiValued = false, description = "The ensemble new password to use (defaults to the old one)")
    private String zookeeperPassword;

    @Option(name = "--zookeeper-ticktime", multiValued = false, description = "The length of a single tick, which is the basic time unit used by ZooKeeper, as measured in milliseconds. It is used to regulate heartbeats, and timeouts. For example, the minimum session timeout will be two ticks")
    private int zooKeeperTickTime;

    @Option(name = "--zookeeper-init-limit", multiValued = false, description = "The amount of time, in ticks (see tickTime), to allow followers to connect and sync to a leader")
    private int zooKeeperInitLimit;

    @Option(name = "--zookeeper-sync-limit", multiValued = false, description = "The amount of time, in ticks (see tickTime), to allow followers to sync with ZooKeeper")
    private int zooKeeperSyncLimit;

    @Option(name = "--zookeeper-data-dir", multiValued = false, description = "The location where ZooKeeper will store the in-memory database snapshots and, unless specified otherwise, the transaction log of updates to the database.")
    private String zooKeeperDataDir;

    @Option(name = "-f", aliases = "--force", multiValued = false, description = "Flag to force the addition without prompt")
    private boolean force = false;

    @Option(name = "--migration-timeout", multiValued = false, description = "Timeout to wait for containers to migrate to the new ensemble")
    private long migrationTimeout = CreateEnsembleOptions.DEFAULT_MIGRATION_TIMEOUT;

    @Argument(required = true, multiValued = true, description = "List of containers to be added")
    private List<String> containers;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateContainersName(containers);
        if (checkIfShouldModify(session, force)) {
            if (containers != null && !containers.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Adding containers:");
                for (String container : containers) {
                    sb.append(" ").append(container);
                }
                sb.append(" to the ensemble. This may take a while.");

                CreateEnsembleOptions.Builder builder = CreateEnsembleOptions.builder();
                applyEnsembleConfiguration();
                builder = builder.zooKeeperServerTickTime(zooKeeperTickTime)
                        .zooKeeperServerInitLimit(zooKeeperInitLimit)
                        .zooKeeperServerSyncLimit(zooKeeperSyncLimit)
                        .zooKeeperServerDataDir(zooKeeperDataDir)
                        .migrationTimeout(migrationTimeout);

                if (generateZookeeperPassword) {
                    //Don't add password
                } else if (zookeeperPassword == null || zookeeperPassword.isEmpty()) {
                    builder = builder.zookeeperPassword(service.getZookeeperPassword());
                } else {
                    builder = builder.zookeeperPassword(zookeeperPassword);
                }

                service.addToCluster(containers, builder.build());
                System.out.println("Updated Zookeeper connection string: " + service.getZooKeeperUrl());
            }
        }
        return null;
    }

    void applyEnsembleConfiguration() throws Exception {
        Map<String, String> currentConfig = service.getEnsembleConfiguration();
        int currentTickTime = Integer.parseInt(currentConfig.get("tickTime"));
        int currentInitLimit = Integer.parseInt(currentConfig.get("initLimit"));
        int currentSyncLimit = Integer.parseInt(currentConfig.get("syncLimit"));
        String currentDataDir = currentConfig.get("dataDir");
        currentDataDir = currentDataDir.substring(0, currentDataDir.lastIndexOf("/"));
        zooKeeperTickTime = zooKeeperTickTime != 0 ? zooKeeperTickTime : currentTickTime;
        zooKeeperInitLimit = zooKeeperInitLimit != 0 ? zooKeeperInitLimit : currentInitLimit;
        zooKeeperSyncLimit = zooKeeperSyncLimit != 0 ? zooKeeperSyncLimit : currentSyncLimit;
        zooKeeperDataDir = !Strings.isNullOrBlank(zooKeeperDataDir) ? zooKeeperDataDir : currentDataDir;
    }

}
