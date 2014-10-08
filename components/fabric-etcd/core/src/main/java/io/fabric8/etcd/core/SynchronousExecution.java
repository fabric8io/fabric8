/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.etcd.core;

import io.fabric8.etcd.api.EtcdException;
import io.fabric8.etcd.api.Response;

import java.util.concurrent.ExecutionException;

public class SynchronousExecution implements Execution<Response> {

    private static final AsynchronousExecution ASYNC = new AsynchronousExecution();

    @Override
    public Response execute(OperationContext context, Operation operation) {
        try {
            return ASYNC.execute(context, operation).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw EtcdException.launderException(e);
        }
    }
}
