/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.fusesource.fabric.git;

import org.codehaus.jackson.annotate.JsonProperty;
import org.fusesource.fabric.groups2.NodeState;

public class GitNode implements NodeState {
	@JsonProperty
	String id;

    @JsonProperty
    String agent;

    @JsonProperty
    String[] services;

	@JsonProperty
	String url;

	/**
	 * The id of the cluster node.  There can be multiple node with this ID,
	 * but only the first node in the cluster will be the master for for it.
	 */
	@Override
	public String id() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String[] getServices() {
        return services;
    }

    public void setServices(String[] services) {
        this.services = services;
    }

    @Override
	public String toString() {
		return "GitNode{" +
				"id='" + id + '\'' +
				", url='" + url + '\'' +
				'}';
	}
}
