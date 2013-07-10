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
package org.fusesource.fabric.itests.paxexam.support;

import org.fusesource.fabric.api.CreateChildContainerOptions;
import org.fusesource.fabric.api.CreateContainerOptionsBuilder;

public class ChildContainerBuilder extends ContainerBuilder<ChildContainerBuilder, CreateChildContainerOptions.Builder> {


	protected ChildContainerBuilder(CreateChildContainerOptions.Builder optionsBuilder) {
		super(optionsBuilder.parent("root").jmxUser("admin").jmxPassword("admin").zookeeperPassword("admin"));
	}

	public static ChildContainerBuilder child() {
		return new ChildContainerBuilder(CreateContainerOptionsBuilder.child());
	}

	public ChildContainerBuilder ofParent(String parent) {
		getOptionsBuilder().parent(parent);
		return this;
	}

	public ChildContainerBuilder asEnsembleServer(boolean setEnsembleServer) {
		getOptionsBuilder().ensembleServer(setEnsembleServer);
		return this;
	}

    public ChildContainerBuilder usingJmxUser(String jmxUser) {
        getOptionsBuilder().jmxUser(jmxUser);
        return this;
    }

    public ChildContainerBuilder usingJmxPassword(String jmxPassword) {
        getOptionsBuilder().jmxPassword(jmxPassword);
        return this;
    }
}
