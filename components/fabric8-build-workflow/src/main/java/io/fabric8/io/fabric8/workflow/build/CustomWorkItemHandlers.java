/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.io.fabric8.workflow.build;

import io.fabric8.io.fabric8.workflow.build.trigger.BuildWorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

/**
 * Registers the custom {@link org.kie.api.runtime.process.WorkItemHandler} instances
 */
public class CustomWorkItemHandlers {
    public static void register(WorkItemManager manager) {
        manager.registerWorkItemHandler("BuildTrigger", new BuildWorkItemHandler());
    }
}
