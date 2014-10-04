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
package io.fabric8.process.manager.support;

import io.fabric8.process.manager.InstallContext;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.config.ProcessConfig;

import java.io.File;

public class CompositeTask implements InstallTask {

    private final InstallTask[] subTasks;

    /**
     * Returns a combined task of task1 and task2 if they are both not null.
     *
     * If one parameter isnot null and the other null then the not null task is returned.
     *
     * Otherwise null is returned if they are both null.
     */
    public static InstallTask combine(InstallTask task1, InstallTask task2) {
        if (task1 != null) {
            if (task2 == null) {
                return task1;
            } else {
                return new CompositeTask(task1, task2);
            }
        } else {
            if (task2 != null) {
                return task2;
            } else {
                return null;
            }
        }
    }

    public CompositeTask(InstallTask... subTasks) {
        this.subTasks = subTasks;
    }

    @Override
    public void install(InstallContext installContext, ProcessConfig config, String id, File installDir) throws Exception {
        for (InstallTask task : subTasks) {
            task.install(installContext, config, id, installDir);
        }
    }
}
