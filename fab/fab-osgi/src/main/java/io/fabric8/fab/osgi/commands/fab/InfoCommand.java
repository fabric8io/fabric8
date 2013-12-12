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

package io.fabric8.fab.osgi.commands.fab;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.fab.DependencyTree;
import io.fabric8.fab.osgi.FabBundleInfo;
import io.fabric8.fab.osgi.FabResolver;
import io.fabric8.fab.osgi.commands.CommandSupport;
import org.fusesource.common.util.Strings;
import org.osgi.framework.Constants;

import java.io.PrintStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import static org.fusesource.common.util.Strings.emptyIfNull;

/**
 * Show some information about what exactly is being installed as part of the FAB: non-shared and shared dependencies,
 * features and features URLs
 */
@Command(name = "info", scope = "fab", description = "Display information about the features and bundles that get installed by a FAB")
public class InfoCommand extends CommandSupport {

    @Argument(index = 0, name = "fab", description = "The Bundle ID, URL or file of the FAB", required = true)
    private String fab;

    @Override
    protected Object doExecute() throws Exception {
        FabResolver resolver = getFabResolver(fab);
        if (fab != null) {
            FabBundleInfo info = resolver.getInfo();

            PrintStream console = session.getConsole();
            console.printf("URL: %s%n", info.getUrl());
            printEmbedded(console, getClassPathElements(info.getManifest().getValue(Constants.BUNDLE_CLASSPATH)));
            printBundles(console, info.getBundles());
            printFeatures(console, info.getFeatures());
            printFeatureURLs(console, info.getFeatureURLs());
            console.printf("%nFor more information about this FAB:%n");
            console.printf("  use 'fab:headers %s' to view the OSGi headers%n", fab);
            console.printf("  use 'fab:tree %s' to view a tree representation of the dependencies%n", fab);
        }
        return null;
    }

    private void printEmbedded(PrintStream console, List<String> elements) {
        if (elements.size() == 0) {
            console.printf("%nNo embedded/non-shared dependencies%n");
        } else {
            console.printf("%nNon-shared dependencies (embedded in FAB):%n");
            for (String element : elements) {
                console.printf("    %s%n", element);
            }
        }
    }

    private void printFeatureURLs(PrintStream console, Collection<URI> uris) {
        if (uris.size() == 0) {
            console.printf("%nNo additional features repositories required%n");
        } else {
            console.printf("%nAdditional features respositories:%n");
            for (URI uri : uris) {
                console.printf("    %s%n", uri);
            }
        }
    }

    private void printFeatures(PrintStream console, Collection<String> features) {
        if (features.size() == 0) {
            console.printf("%nNo additional features required%n");
        } else {
            console.printf("%nAdditional features:%n");
            for (String feature : features) {
                console.printf("    %s%n", feature);
            }
        }
    }

    private void printBundles(PrintStream console, Collection<DependencyTree> bundles) {
        if (bundles.size() == 0) {
            console.printf("%nNo shared/bundle dependencies required%n");
        } else {
            console.printf("%nShared dependencies (installed as bundles when necessary):%n");
            for (DependencyTree bundle : bundles) {
                console.printf("    mvn:%s/%s/%s%n", bundle.getGroupId(), bundle.getArtifactId(), bundle.getVersion());
            }
        }
    }

    protected static List<String> getClassPathElements(String value) {
        List<String> list = Strings.splitAndTrimAsList(emptyIfNull(value), ",");
        list.remove(".");
        return list;
    }
}
