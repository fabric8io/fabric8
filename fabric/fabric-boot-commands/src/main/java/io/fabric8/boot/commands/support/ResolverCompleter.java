/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.fabric8.boot.commands.support;

import java.util.Arrays;
import java.util.List;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;


import static io.fabric8.zookeeper.ZkDefs.LOCAL_HOSTNAME;
import static io.fabric8.zookeeper.ZkDefs.LOCAL_IP;
import static io.fabric8.zookeeper.ZkDefs.PUBLIC_HOSTNAME;
import static io.fabric8.zookeeper.ZkDefs.PUBLIC_IP;
import static io.fabric8.zookeeper.ZkDefs.MANUAL_IP;


public class ResolverCompleter implements Completer {

    StringsCompleter delegate = new StringsCompleter();

    @Override
    public int complete(String buffer, int index, List<String> candidates) {
        delegate.getStrings().addAll(Arrays.asList(LOCAL_HOSTNAME,LOCAL_IP,PUBLIC_HOSTNAME,PUBLIC_IP, MANUAL_IP));
        return delegate.complete(buffer, index, candidates);
    }
}
