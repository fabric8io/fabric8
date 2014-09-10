/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.internal.cluster;

import io.fabric8.api.Container;
import io.fabric8.api.CreateEnsembleOptions;
import org.apache.curator.framework.api.ACLProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ZooKeeperClusterOperationContext {

    public  static class Builder {
        private ZooKeeperClusterState currentState;
        private ZooKeeperClusterState targetState;
        private List<Container> containersToAdd;
        private List<Container> containersToRemove;
        private Map<String, Container> allContainers;
        private CreateEnsembleOptions createEnsembleOptions;
        private CountDownLatch countDownLatch;
        private String usersname;
        private String password;
        private ACLProvider aclProvider;

        public Builder currentState(final ZooKeeperClusterState currentState) {
            this.currentState = currentState;
            return this;
        }

        public Builder targetState(final ZooKeeperClusterState targetState) {
            this.targetState = targetState;
            return this;
        }

        public Builder containersToAdd(final List<Container> containersToAdd) {
            this.containersToAdd = containersToAdd;
            return this;
        }

        public Builder containersToRemove(final List<Container> containersToRemove) {
            this.containersToRemove = containersToRemove;
            return this;
        }

        public Builder allContainers(final Map<String, Container> allContainers) {
            this.allContainers = allContainers;
            return this;
        }

        public Builder createEnsembleOptions(final CreateEnsembleOptions createEnsembleOptions) {
            this.createEnsembleOptions = createEnsembleOptions;
            return this;
        }

        public Builder countDownLatch(final CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
            return this;
        }

        public Builder usersname(final String usersname) {
            this.usersname = usersname;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder aclProvider(final ACLProvider aclProvider) {
            this.aclProvider = aclProvider;
            return this;
        }

        public ZooKeeperClusterOperationContext build() {
            return new ZooKeeperClusterOperationContext(currentState, targetState, containersToAdd, containersToRemove,
                    allContainers, createEnsembleOptions, countDownLatch, usersname, password, aclProvider);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final ZooKeeperClusterState currentState;
    private final ZooKeeperClusterState targetState;
    private final List<Container> containersToAdd;
    private final List<Container> containersToRemove;
    private final Map<String, Container> allContainers;
    private final CreateEnsembleOptions createEnsembleOptions;
    private final CountDownLatch countDownLatch;
    private final String usersname;
    private final String password;
    private final ACLProvider aclProvider;

    private ZooKeeperClusterOperationContext(ZooKeeperClusterState currentState, ZooKeeperClusterState targetState, List<Container> containersToAdd, List<Container> containersToRemove, Map<String, Container> allContainers, CreateEnsembleOptions createEnsembleOptions, CountDownLatch countDownLatch, String usersname, String password, ACLProvider aclProvider) {
        this.currentState = currentState;
        this.targetState = targetState;
        this.containersToAdd = containersToAdd;
        this.containersToRemove = containersToRemove;
        this.allContainers = allContainers;
        this.createEnsembleOptions = createEnsembleOptions;
        this.countDownLatch = countDownLatch;
        this.usersname = usersname;
        this.password = password;
        this.aclProvider = aclProvider;
    }

    public ZooKeeperClusterState getCurrentState() {
        return currentState;
    }

    public ZooKeeperClusterState getTargetState() {
        return targetState;
    }

    public List<Container> getContainersToAdd() {
        return containersToAdd;
    }

    public List<Container> getContainersToRemove() {
        return containersToRemove;
    }

    public Map<String, Container> getAllContainers() {
        return allContainers;
    }

    public CreateEnsembleOptions getCreateEnsembleOptions() {
        return createEnsembleOptions;
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public String getUsersname() {
        return usersname;
    }

    public String getPassword() {
        return password;
    }

    public ACLProvider getAclProvider() {
        return aclProvider;
    }
}
