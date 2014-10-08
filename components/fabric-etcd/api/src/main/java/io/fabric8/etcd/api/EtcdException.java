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

package io.fabric8.etcd.api;

import java.util.concurrent.ExecutionException;

public class EtcdException extends RuntimeException {

    private final int errorCode;

    public EtcdException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public EtcdException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public EtcdException(Throwable cause, int errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public EtcdException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int errorCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public static RuntimeException launderException(Throwable t) {
        if (t instanceof EtcdException) {
            return (EtcdException) t;
        } else if (t instanceof ExecutionException) {
            return launderException(t.getCause());
        } else if (t instanceof RuntimeException) {
            return  (RuntimeException) t;
        }
        return new RuntimeException(t);
    }
}
