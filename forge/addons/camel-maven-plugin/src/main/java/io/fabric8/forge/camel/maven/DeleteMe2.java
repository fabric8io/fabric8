/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.camel.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.fabric8.forge.camel.commands.project.helper.RouteBuilderParser;
import io.fabric8.forge.camel.commands.project.model.CamelEndpointDetails;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
//import org.apache.camel.catalog.EndpointValidationResult;
//import org.apache.camel.catalog.lucene.LuceneSuggestionStrategy;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

// TODO: This works with just roaster

public class DeleteMe2 {

    public static void main(String[] args) throws Exception {
        CamelCatalog catalog = new DefaultCamelCatalog();
        //catalog.setSuggestionStrategy(new LuceneSuggestionStrategy());

        JavaClassSource clazz = (JavaClassSource) Roaster.parse(new File("src/main/java/io/fabric8/forge/camel/maven/MyRouteBuilder.java"));
        List<CamelEndpointDetails> endpoints = new ArrayList<>();

        String fqn = "src/main/java/io/fabric8/forge/camel/maven/MyRouteBuilder.java";
        String baseDir = ".";
        RouteBuilderParser.parseRouteBuilder(clazz, baseDir, fqn, endpoints);

        // TODO: requires Camel 2.16.2+
        System.out.println(endpoints);
        /*for (CamelEndpointDetails detail : endpoints) {
            EndpointValidationResult result = catalog.validateEndpointProperties(detail.getEndpointUri());
            if (!result.isSuccess()) {
                String out = result.summaryErrorMessage();
                System.out.println(out);
            }
        }*/
    }
}
