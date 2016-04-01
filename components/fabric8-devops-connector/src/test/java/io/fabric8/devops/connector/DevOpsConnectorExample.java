/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.devops.connector;

import java.io.File;

public class DevOpsConnectorExample {

    /**
     *
     * DevOps connector Example that runs against a live openshift environment and automatically picks which env to run against using ~/kube/config.
     * Ideal for running against the fabric8 vagrant image.
     *
     * Jenkins is required to be running.
     *
     * Use the following command to delete openshift builds and build configs.  It will also build this Example class and run it..
     *
     * oc delete bc --all && oc delete builds --all && mvn clean test &&  mvn exec:java -Dexec.mainClass="io.fabric8.devops.connector.DevOpsConnectorExample" -Dexec.classpathScope=test
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        DevOpsConnector devops = new DevOpsConnector();

        devops.setBasedir(new File("./"));
        devops.setGitUrl("someURL");

        devops.execute();

        System.out.println("Finished");
    }

}
