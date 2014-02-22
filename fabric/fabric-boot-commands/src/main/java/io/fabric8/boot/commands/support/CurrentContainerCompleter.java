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

import java.util.List;

import io.fabric8.api.RuntimeProperties;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import io.fabric8.utils.SystemProperties;

public class CurrentContainerCompleter implements Completer {

    protected RuntimeProperties runtimeProperties;
    private StringsCompleter delegate = new StringsCompleter();

    @Override
    public int complete(String s, int i, List<String> strings) {
        delegate.getStrings().clear();
        delegate.getStrings().add(runtimeProperties.getProperty(SystemProperties.KARAF_NAME));
        return delegate.complete(s,i,strings);
    }

    public RuntimeProperties getRuntimeProperties() {
        return runtimeProperties;
    }

    public void setRuntimeProperties(RuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }
}
