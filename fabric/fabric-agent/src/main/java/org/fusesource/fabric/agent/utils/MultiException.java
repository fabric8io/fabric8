/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
