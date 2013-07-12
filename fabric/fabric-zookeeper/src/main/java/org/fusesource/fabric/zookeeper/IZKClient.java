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

import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.linkedin.util.clock.Timespan;

/**
 * Extended zookeeper interface
 */
@Deprecated
public interface IZKClient extends org.linkedin.zookeeper.client.IZKClient {

    Stat createOrSetByteWithParents(String path, byte[] data, List<ACL> acl, CreateMode createMode)
            throws InterruptedException, KeeperException;

    // ACL free methods

    String create(String path, CreateMode createMode) throws InterruptedException, KeeperException;

    String create(String path, String data, CreateMode createMode) throws InterruptedException, KeeperException;

    String create(String path, byte[] data, CreateMode createMode) throws InterruptedException, KeeperException;

    String createWithParents(String path, CreateMode createMode) throws InterruptedException, KeeperException;

    String createWithParents(String path, String data, CreateMode createMode) throws InterruptedException, KeeperException;

    String createWithParents(String path, byte[] data, CreateMode createMode) throws InterruptedException, KeeperException;

    Stat createOrSetWithParents(String path, String data, CreateMode createMode) throws InterruptedException, KeeperException;

    Stat createOrSetWithParents(String path, byte[] data, CreateMode createMode) throws InterruptedException, KeeperException;

    // State methods

    void waitForConnected(Timespan timeout) throws InterruptedException, TimeoutException;

    void waitForConnected() throws InterruptedException, TimeoutException;

    // ACLs methods

    void fixACLs(String path, boolean recursive) throws InterruptedException, KeeperException;

}
