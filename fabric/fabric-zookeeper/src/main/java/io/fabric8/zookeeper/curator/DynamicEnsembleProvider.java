
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.fabric8.zookeeper.curator;

import org.apache.curator.ensemble.EnsembleProvider;

import java.io.IOException;

public class DynamicEnsembleProvider implements EnsembleProvider {

    private String connectionString;

    public void update(String connectionString) {
        this.connectionString = connectionString;
    }

    @Override
    public String getConnectionString() {
        return connectionString;
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void close() throws IOException {
    }
}