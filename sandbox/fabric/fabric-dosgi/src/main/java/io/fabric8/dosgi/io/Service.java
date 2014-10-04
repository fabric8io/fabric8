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
package io.fabric8.dosgi.io;


/**
 * The core lifecyle interface for ActiveMQ components.
 *
 * @version $Revision: 1.1 $
 */
public interface Service {

    /**
     * Starts the service.  No guarantee is given that the service has fully started
     * by the time this method returns.
     */
    void start() throws Exception;

    /**
     * Starts the service.  Executes the onComplete runnable once the service has fully started up.
     *
     * @param onComplete my be set to null if not interested in a callback.
     */
    void start(Runnable onComplete) throws Exception;

    /**
     * Stops the service.  No guarantee is given that the service has fully stopped
     * by the time this method returns.
     */
    void stop();

    /**
     * Stops the service.  Executes the onComplete runnable once the service has fully stopped.
     *
     * @param onComplete my be set to null if not interested in a callback.
     */
    void stop(Runnable onComplete);

}
