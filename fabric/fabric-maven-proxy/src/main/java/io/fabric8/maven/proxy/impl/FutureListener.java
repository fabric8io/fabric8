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
package io.fabric8.maven.proxy.impl;

/**
 * Something interested in being notified when the completion
 * of an asynchronous download operation : {@link DefaultFuture}.
 *
 */
public interface FutureListener<T extends DefaultFuture> {

    /**
     * Invoked when the operation associated with the {@link DefaultFuture}
     * has been completed even if you add the listener after the completion.
     *
     * @param future The source {@link DefaultFuture} which called this
     *               callback.
     */
    void operationComplete(T future);

}
