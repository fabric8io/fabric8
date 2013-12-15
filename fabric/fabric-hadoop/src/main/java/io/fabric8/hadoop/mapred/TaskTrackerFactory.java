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
package io.fabric8.hadoop.mapred;

import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TaskTracker;
import org.apache.hadoop.util.Daemon;
import io.fabric8.hadoop.Factory;

public class TaskTrackerFactory extends Factory<TaskTracker> {

    @Override
    protected TaskTracker doCreate(Dictionary properties) throws Exception {
        JobConf conf = new JobConf();
        for (Enumeration e = properties.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            Object val = properties.get(key);
            conf.set( key.toString(), val.toString() );
        }
        TaskTracker taskTracker = new TaskTracker(conf);
        new Daemon(taskTracker).start();
        return taskTracker;
    }

    @Override
    protected void doDelete(TaskTracker service) throws Exception {
        service.shutdown();
    }

}
