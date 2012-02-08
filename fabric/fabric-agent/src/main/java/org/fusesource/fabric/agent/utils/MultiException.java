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
import java.util.Collections;
import java.util.List;

public class MultiException extends Exception {

    private List<Throwable> causes;

    public MultiException(String message, List<Throwable> causes) {
        super(message);
        this.causes = causes != null ? causes : Collections.<Throwable>emptyList();
    }

    /* ------------------------------------------------------------ */
    @Override
    public String toString()
    {
        return MultiException.class.getSimpleName() + causes.toString();
    }

    /* ------------------------------------------------------------ */
    @Override
    public void printStackTrace()
    {
        super.printStackTrace();
        for (Throwable t : causes)
            t.printStackTrace();
    }


    /* ------------------------------------------------------------------------------- */
    /**
     * @see java.lang.Throwable#printStackTrace(java.io.PrintStream)
     */
    @Override
    public void printStackTrace(PrintStream out)
    {
        super.printStackTrace(out);
        for (Throwable t : causes)
            t.printStackTrace(out);
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * @see java.lang.Throwable#printStackTrace(java.io.PrintWriter)
     */
    @Override
    public void printStackTrace(PrintWriter out)
    {
        super.printStackTrace(out);
        for (Throwable t : causes)
            t.printStackTrace(out);
    }

}
