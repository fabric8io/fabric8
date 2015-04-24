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

public class ImmutableResponse implements Response<ImmutableNode> {

    public final String action;
    public final ImmutableNode node;
    public final ImmutableNode prevNode;

    public final int errorCode;
    public final String message;
    public final String cause;
    public final int errorIndex;

    public ImmutableResponse(Response response) {
        this.action = response.getAction();
        this.node = response.getNode() != null ? new ImmutableNode(response.getNode()) : null;
        this.prevNode = response.getPrevNode() != null ? new ImmutableNode(response.getPrevNode()) : null;
        this.errorCode = response.getErrorCode();
        this.message = response.getMessage();
        this.cause = response.getCause();
        this.errorIndex = response.getErrorIndex();
    }

    public ImmutableResponse(String action, ImmutableNode node, ImmutableNode prevNode, int errorCode, String message, String cause, int errorIndex) {
        this.action = action;
        this.node = node;
        this.prevNode = prevNode;
        this.errorCode = errorCode;
        this.message = message;
        this.cause = cause;
        this.errorIndex = errorIndex;
    }

    public String getAction() {
        return action;
    }

    public ImmutableNode getNode() {
        return node;
    }

    public ImmutableNode getPrevNode() {
        return prevNode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getCause() {
        return cause;
    }

    public int getErrorIndex() {
        return errorIndex;
    }

    @Override
    public String toString() {
        return "ImmutableResponse{" +
                "action='" + action + '\'' +
                ", node=" + node +
                ", prevNode=" + prevNode +
                ", errorCode=" + errorCode +
                ", message='" + message + '\'' +
                ", cause='" + cause + '\'' +
                ", errorIndex=" + errorIndex +
                '}';
    }
}
