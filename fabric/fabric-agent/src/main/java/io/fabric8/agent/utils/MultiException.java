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
package io.fabric8.agent.utils;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiException extends Exception {

    private Collection<Throwable> causes = new ArrayList<Throwable>();

    public MultiException(String message) {
        super(message);
    }

    public MultiException(String message, Collection<Throwable> causes) {
        super(message);
        this.causes.addAll(causes);
    }

    public void addCause(Throwable e) {
        causes.add(e);
    }

    public void throwIfCauses() throws MultiException {
        if (!causes.isEmpty()) {
            throw this;
        }
    }

    public Throwable[] getCauses() {
        return causes.toArray(new Throwable[causes.size()]);
    }

    @Override
    public void printStackTrace()
    {
        super.printStackTrace();
        for (Throwable t : causes) {
            t.printStackTrace();
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
        for (Throwable t : causes) {
            t.printStackTrace(out);
        }
    }

    @Override
    public void printStackTrace(PrintWriter out)
    {
        super.printStackTrace(out);
        for (Throwable t : causes) {
            t.printStackTrace(out);
        }
    }

    public static void throwIf(String message, List<Throwable> throwables) throws MultiException {
        if (throwables != null && !throwables.isEmpty()) {
            StringBuilder sb = new StringBuilder(message);
            sb.append(":");
            for (Throwable t : throwables) {
                sb.append("\n\t");
                sb.append(t.getMessage());
            }
            throw new MultiException(sb.toString(), throwables);
        }
    }
}
