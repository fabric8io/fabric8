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
package io.fabric8.etcd.core;

import io.fabric8.etcd.api.Response;

public class MutableResponse implements Response<MutableNode> {

    public String action;
    public MutableNode node;
    public MutableNode prevNode;

    public int errorCode;
    public String message;
    public String cause;
    public int errorIndex;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public MutableNode getNode() {
        return node;
    }

    public void setNode(MutableNode node) {
        this.node = node;
    }

    public MutableNode getPrevNode() {
        return prevNode;
    }

    public void setPrevNode(MutableNode prevNode) {
        this.prevNode = prevNode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public int getErrorIndex() {
        return errorIndex;
    }

    public void setErrorIndex(int errorIndex) {
        this.errorIndex = errorIndex;
    }
}
