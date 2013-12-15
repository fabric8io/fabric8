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

package io.fabric8.internal;

import java.io.PrintStream;
import io.fabric8.api.CreationStateListener;

public class PrintStreamCreationStateListener implements CreationStateListener {

    PrintStream printStream;

    public PrintStreamCreationStateListener(PrintStream printStream) {
        this.printStream = printStream;
    }

    /**
     * Called when the state is changed.
     *
     * @param message The message describing the current state of the creation process.
     */
    @Override
    public void onStateChange(String message) {
        printStream.println(message);
    }
}
