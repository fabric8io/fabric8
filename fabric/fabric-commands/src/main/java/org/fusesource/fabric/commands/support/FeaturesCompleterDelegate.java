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

package org.fusesource.fabric.commands.support;

import java.util.List;
import org.apache.karaf.shell.console.Completer;

/**
 * A delegating completer to Karafs AllFeature completer.
 * The purpose of this class is to provide a completer that will work even if Feature service/commands are not persent.
 */
public class FeaturesCompleterDelegate implements Completer {

    public static Completer DELEGATE;

    @Override
    public int complete(String s, int i, List<String> strings) {
        if (DELEGATE != null) {
            return DELEGATE.complete(s,i,strings);
        }
        return 0;
    }
}
