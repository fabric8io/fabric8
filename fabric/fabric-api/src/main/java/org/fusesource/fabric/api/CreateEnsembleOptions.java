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
package org.fusesource.fabric.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CreateEnsembleOptions implements Serializable{

    private String zookeeperPassword;
    private final Map<String, String> users = new HashMap<String, String>();

    public static CreateEnsembleOptions build() {
        return new CreateEnsembleOptions();
    }

    public CreateEnsembleOptions zookeeperPassword(final String zookeeperPassword) {
        this.setZookeeperPassword(zookeeperPassword);
        return this;
    }

    public CreateEnsembleOptions user(final String user, final String password) {
        if (user != null && !user.isEmpty()) {
            this.getUsers().put(user, password);
        }
        return this;
    }

    public String getZookeeperPassword() {
        return zookeeperPassword;
    }

    public void setZookeeperPassword(String zookeeperPassword) {
        this.zookeeperPassword = zookeeperPassword;
    }

    public Map<String, String> getUsers() {
        return users;
    }
}
