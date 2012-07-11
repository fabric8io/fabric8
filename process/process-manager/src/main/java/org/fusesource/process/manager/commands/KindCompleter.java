/*
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

package org.fusesource.process.manager.commands;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.fusesource.process.manager.Installation;
import org.fusesource.process.manager.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Loads the controllerKinds on the classpath and uses is to complete on the kinds of controller available
 */
public class KindCompleter implements Completer {
    private static final transient Logger LOG = LoggerFactory.getLogger(KindCompleter.class);

    private List<String> kinds = Lists.newArrayList();

    public void init() {
        // lets load the kinds from the classpath
        InputStream in = ProcessManager.class.getResourceAsStream("controllerKinds");
        if (in == null) {
            LOG.warn("Cannot find controllerKinds on the classpath!");
        } else {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in,  Charsets.UTF_8));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) break;
                    line = line.trim();
                    if (!line.isEmpty()) {
                        kinds.add(line);
                    }
                }
            } catch (IOException e) {
                Closeables.closeQuietly(in);
            }
        }
    }

    @Override
    public int complete(final String buffer, final int cursor, final List candidates) {
        StringsCompleter delegate = new StringsCompleter(kinds);
        return delegate.complete(buffer, cursor, candidates);
    }
}
