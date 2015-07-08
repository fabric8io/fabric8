/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.build.workitems;

import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Triggers a build in OpenShift using the CDelivery remote REST Service to avoid
 * embedding much code inside jBPM Designer
 */
public class BuildWorkItemHandler implements WorkItemHandler {
    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        Map<String, Object> parameters = workItem.getParameters();
        Map<String, Object> allParameters = new HashMap<String, Object>(parameters);
        allParameters.put("processInstanceId", workItem.getProcessInstanceId());
        allParameters.put("workItemId", workItem.getId());

        // lets find the namespace and buildName from the name if there's no input parameters

        if (!allParameters.containsKey("namespace") || !allParameters.containsKey("buildName")) {
            String name = workItem.getName();
            if (name != null) {
                String[] pair = name.split("/", 2);
                if (pair != null && pair.length == 2) {
                    String namespace = pair[0];
                    String buildName = pair[1];
                    allParameters.put("namespace", namespace);
                    allParameters.put("buildName", buildName);
                }
            }
        }

        if (!allParameters.containsKey("namespace") || !allParameters.containsKey("buildName")) {
            StartProcessWorkItemHandler.logWarning("Should have namespace and buildName but has " + allParameters);
        }

        // lets turn it into JSON
        StringBuilder buffer = new StringBuilder("{");
        int count = 0;
        Set<Map.Entry<String, Object>> entries = allParameters.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key != null && value != null) {
                if (count++ > 0) {
                    buffer.append(", ");
                }
                encodeJson(buffer, key);
                buffer.append(": ");
                encodeJson(buffer, value);
            }
        }
        buffer.append("}");

        String json = buffer.toString();
        System.out.println("About to post JSON: " + json);

        String triggerBuildURL = createTriggerBuildURL();
        try {
            URL url = new URL(triggerBuildURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            OutputStreamWriter out = new OutputStreamWriter(
                    connection.getOutputStream());
            out.write(json);
            out.close();

/*

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String decodedString;
            while ((decodedString = in.readLine()) != null) {
                System.out.println(decodedString);
            }
            in.close();
*/
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            System.out.println("Got response code: " + responseCode + " message: "+ responseMessage);
        } catch (IOException e) {
            StartProcessWorkItemHandler.logError("Failed to post to: " + triggerBuildURL + ". " + e, e);
        }
    }

    protected String createTriggerBuildURL() {
        String host = System.getenv("CDELIVERY_SERVICE_HOST");
        if (host == null) {
            host = "localhost";
        }
        String port = System.getenv("CDELIVERY_SERVICE_PORT");
        if (port == null) {
            port = "8787";
        }
        return "http://" + host + (port.length() > 0 ? ":" : "") + port + "/triggerBuild";
    }

    protected void encodeJson(StringBuilder buffer, Object value) {
        if (value instanceof Boolean || value instanceof Number) {
            buffer.append(value.toString());
        } else {
            // lets assume a string
            buffer.append('"');
            buffer.append(value.toString());
            buffer.append('"');
        }
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    }
}
