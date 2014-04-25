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
package io.fabric8.boot.commands.support;

import static io.fabric8.zookeeper.ZkDefs.LOCAL_HOSTNAME;
import static io.fabric8.zookeeper.ZkDefs.LOCAL_IP;
import static io.fabric8.zookeeper.ZkDefs.MANUAL_IP;
import static io.fabric8.zookeeper.ZkDefs.PUBLIC_HOSTNAME;
import static io.fabric8.zookeeper.ZkDefs.PUBLIC_IP;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

@Component(immediate = true)
@Service({ ResolverCompleter.class, Completer.class })
public final class ResolverCompleter extends AbstractCompleterComponent {

    StringsCompleter delegate = new StringsCompleter();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getParameter() {
        return "--resolver";
    }

    @Override
    public int complete(String buffer, int index, List<String> candidates) {
        delegate.getStrings().addAll(Arrays.asList(LOCAL_HOSTNAME, LOCAL_IP, PUBLIC_HOSTNAME, PUBLIC_IP, MANUAL_IP));
        return delegate.complete(buffer, index, candidates);
    }
}
