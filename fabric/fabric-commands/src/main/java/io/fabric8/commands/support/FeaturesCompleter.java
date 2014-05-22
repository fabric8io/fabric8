/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.commands.support;

import java.util.List;

import io.fabric8.api.scr.AbstractComponent;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.AllFeatureCompleter;
import org.apache.karaf.shell.console.Completer;

@Component(immediate = true)
@Service({FeaturesCompleter.class, Completer.class})
public class FeaturesCompleter extends AbstractComponent implements Completer {

    @Reference
    private FeaturesService featuresService;

    private AllFeatureCompleter delegate;

    @Activate
    void activate() {
        delegate = new AllFeatureCompleter();
        delegate.setFeaturesService(featuresService);
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        return delegate.complete(buffer, cursor, candidates);
    }
}
