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
package org.fusesource.fabric.agent.utils;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class MultiException extends Exception {

    private List<Exception> exceptions = new ArrayList<Exception>();

    public MultiException(String message) {
        super(message);
    }

    public MultiException(String message, List<Exception> exceptions) {
        super(message);
        this.exceptions = exceptions;
    }

    public void addException(Exception e) {
        exceptions.add(e);
    }

    public void throwIfExceptions() throws MultiException {
        if (!exceptions.isEmpty()) {
            throw this;
        }
    }

    public Throwable[] getCauses() {
        return exceptions.toArray(new Throwable[exceptions.size()]);
    }

    @Override
    public void printStackTrace()
    {
        super.printStackTrace();
        for (Exception e : exceptions) {
            e.printStackTrace();
        }
    }


    /* ------------------------------------------------------------------------------- */
    /**
     * @see java.lang.Throwable#printStackTrace(java.io.PrintStream)
     */
    @Override
    public void printStackTrace(PrintStream out)
    {
        super.printStackTrace(out);
        for (Exception e : exceptions) {
            e.printStackTrace(out);
        }
    }

    @Override
    public void printStackTrace(PrintWriter out)
    {
        super.printStackTrace(out);
        for (Exception e : exceptions) {
            e.printStackTrace(out);
        }
    }

    public static void throwIf(String message, List<Exception> exceptions) throws MultiException {
        if (exceptions != null && !exceptions.isEmpty()) {
            StringBuilder sb = new StringBuilder(message);
            sb.append(":");
            for (Exception e : exceptions) {
                sb.append("\n\t");
                sb.append(e.getMessage());
            }
            throw new MultiException(sb.toString(), exceptions);
        }
    }
}
