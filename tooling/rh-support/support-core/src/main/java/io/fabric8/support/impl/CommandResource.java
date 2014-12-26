package io.fabric8.support.impl;

import io.fabric8.support.api.Resource;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.fusesource.jansi.AnsiOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * {@link io.fabric8.support.api.Resource} implementation that executes a Karaf shell command
 * and captures the output.
 */
class CommandResource implements Resource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandResource.class);
    private static final long DELAY = 5000l;

    private final CommandProcessor processor;
    private final String command;

    public CommandResource(CommandProcessor processor, String command) {
        super();
        this.processor = processor;
        this.command = command;
    }

    @Override
    public void write(OutputStream os) {
        LOGGER.info("Adding output of command '{}' to support information (file name {})", command, getName());
        executeCommand(command, os);
    }

    @Override
    public String getName() {
        return String.format("commands/%s", command.replaceAll("\\|", "PIPE").replaceAll("[^a-zA-Z0-9-_\\.]", "_"));
    }

    private void executeCommand(final String command, final OutputStream os) {
        final PrintStream out = new PrintStream(new AnsiOutputStream(os));
        out.printf("Command: %s%n", command);
        out.printf("--------------------%n");
        final CommandSession session = processor.createSession(System.in, out, out);
        Future<Void> future = Executors.newSingleThreadExecutor().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    session.execute(command);
                } catch (Exception e) {
                    LOGGER.warn("Exception while collecting support information - error running command '{}'", command);
                    e.printStackTrace(out);
                }
                return null;
            }
        });

        try {
            future.get(DELAY, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.warn(String.format("Exception while collecting support information - error waiting for command '{}'", command), e);
            future.cancel(true);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
