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
package io.fabric8.taiga;

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;

import java.util.List;

import static io.fabric8.utils.cxf.JsonHelper.toJson;

/**
 */
public class Example {
    public static void main(String[] args) {
        String userName = Systems.getEnvVarOrSystemProperty("TAIGA_USERNAME", "admin");
        String password = Systems.getEnvVarOrSystemProperty("TAIGA_PASSWORD", "123123");

        String projectName = "admin-beer";
        if (args.length > 0) {
            projectName = args[0];
        }

        try {
            KubernetesClient kubernetes = new KubernetesClient();
            String namespace = kubernetes.getNamespace();
            String address = kubernetes.getServiceURL(ServiceNames.TAIGA, namespace, "http", true);
            if (Strings.isNullOrBlank(address)) {
                System.out.println("No Taiga service could be found in kubernetes " + namespace + " on address: " + kubernetes.getAddress());
                return;
            }
            System.out.println("Logging into taiga at " + address + " as user " + userName);
            TaigaClient client = new TaigaClient(address, userName, password);


            ProjectDTO myProject = client.getProjectBySlug(projectName);
            System.out.println("Found project: " + myProject + " by slug: " + projectName);

            System.out.println("Project id for slug: " + myProject + " is: " + client.getProjectIdForSlug(projectName));

            ProjectDTO notExist = client.getProjectBySlug("does-not-exist");
            System.out.println("Found non existing project: " + notExist);

            ProjectDTO autoCreateProject = client.getOrCreateProjectBySlug("admin-thingy", "thingy");
            System.out.println("getOrCreateProject: " + autoCreateProject);


            List<ProjectDTO> projects = client.getProjects();
            for (ProjectDTO project : projects) {
                System.out.println("Project " + project.getId() + " has slug: " + project.getSlug() + " name " + project.getName());
            }
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }
}
