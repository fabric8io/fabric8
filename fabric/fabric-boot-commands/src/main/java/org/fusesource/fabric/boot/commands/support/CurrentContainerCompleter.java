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

package org.fusesource.fabric.boot.commands.support;

import java.util.List;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.fusesource.fabric.utils.SystemProperties;

public class CurrentContainerCompleter implements Completer {

    private StringsCompleter delegate = new StringsCompleter();

    @Override
    public int complete(String s, int i, List<String> strings) {
        delegate.getStrings().clear();
        delegate.getStrings().add(System.getProperty(SystemProperties.KARAF_NAME));
        return delegate.complete(s,i,strings);
    }
}
