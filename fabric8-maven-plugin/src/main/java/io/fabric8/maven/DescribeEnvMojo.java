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
package io.fabric8.maven;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import io.fabric8.maven.support.JsonSchema;
import io.fabric8.maven.support.JsonSchemaProperty;
import io.fabric8.utils.TablePrinter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Displays the currently known environment variables for the container using
 * known maven properties and any detected <code>io/fabric8/environment/schema.json</code> files
 * found on the classpath
 */
@Mojo(name = "describe-env", defaultPhase = LifecyclePhase.COMPILE)
public class DescribeEnvMojo extends AbstractFabric8Mojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            JsonSchema schema = getEnvironmentVariableJsonSchema();
            displaySchema(schema);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load environment schemas: " + e, e);
        }
    }

    protected void displaySchema(JsonSchema schema) {
        TablePrinter table = new TablePrinter();
        table.columns("Name", "Default", "Type", "Description");
        Map<String, JsonSchemaProperty> properties = schema.getProperties();
        SortedMap<String, JsonSchemaProperty> sortedProperties = new TreeMap<>();
        if (properties != null) {
            sortedProperties.putAll(properties);
        }
        Set<Map.Entry<String, JsonSchemaProperty>> entries = sortedProperties.entrySet();
        for (Map.Entry<String, JsonSchemaProperty> entry : entries) {
            String name = entry.getKey();
            JsonSchemaProperty property = entry.getValue();
            table.row(name, property.getDefaultValue(), property.getType(), property.getDescription());
        }

        getLog().info("");
        getLog().info("Environment variables which can be injected:");
        getLog().info("--------------------------------------------");

        List<String> lines = table.asTextLines();
        for (String line : lines) {
            getLog().info(line);
        }
        getLog().info("");
    }


}
