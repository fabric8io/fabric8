/*
 * Copyright (C) 2011 FuseSource, Corp. All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the CDDL license
 * a copy of which has been included with this distribution in the license.txt file.
 */

package org.fusesource.fabric.itests.paxexam;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.ProbeBuilder;
import org.osgi.framework.Constants;

public class FabricCommandsTestSupport extends FabricTestSupport {

    /**
     * Executes the command and returns the output as a String.
     *
     * @param command
     * @return
     */
    protected String executeCommand(String command) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
        CommandSession commandSession = commandProcessor.createSession(System.in, printStream, System.err);
        try {
            commandSession.execute(command);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return byteArrayOutputStream.toString();
    }

    /**
     * This is used to customize the Probe that will contain the test.
     * We need to enable dynamic import of provisional bundles, to use the Console.
     *
     * @param probe
     * @return
     */
    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

}
