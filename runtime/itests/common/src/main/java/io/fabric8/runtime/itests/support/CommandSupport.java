/*
 * #%L
 * JBossOSGi SPI
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package io.fabric8.runtime.itests.support;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.junit.Assert;

/**
 * Test helper utility
 *
 * @author thomas.diesler@jboss.com
 * @since 03-Feb-2014
 */
public final class CommandSupport {

    // Hide ctor
    private CommandSupport() {
    }

    public static String executeCommands(String... commands) throws Exception {
        StringBuffer aggregated = new StringBuffer();
        for (String cmdstr : commands) {
            String result = executeCommand(cmdstr, new DummyCommandSession());
            if (result != null) {
                aggregated.append(result);
            }
            return result;
        }
        return aggregated.length() > 0 ? aggregated.toString() : null;
    }

    public static String executeCommand(String cmdstr) throws Exception {
        return executeCommand(cmdstr, new DummyCommandSession());
    }

    /**
     * Execute a command in process by direct lookup/invocation of the associated command service
     * The osgi.command.scope and osgi.command.function parameter values are derived from
     * the first commadn string token
     */
    public static String executeCommand(String cmdstr, CommandSession session) throws Exception {
        List<String> tokens = Arrays.asList(cmdstr.split("\\s"));
        String[] header = tokens.get(0).split(":");
        Assert.assertTrue("Two tokens", header.length == 2);
        String filter = "(&(osgi.command.scope=" + header[0] + ")(osgi.command.function=" + header[1] + "))";
        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        AbstractCommand command = (AbstractCommand) ServiceLocator.awaitService(moduleContext, Function.class, filter);
        List<Object> args = new ArrayList<Object>(tokens);
        args.remove(0);
        Object result = command.execute(session, args);
        return result != null ? result.toString() : null;
    }

    public static class DummyCommandSession implements CommandSession {

        @Override
        public void close() {
        }

        @Override
        public Object convert(Class<?> arg0, Object arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object execute(CharSequence arg0) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharSequence format(Object arg0, int arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object get(String arg0) {
            return null;
        }

        @Override
        public void put(String arg0, Object arg1) {
            // do nothing
        }

        @Override
        public PrintStream getConsole() {
            return System.out;
        }

        @Override
        public InputStream getKeyboard() {
            throw new UnsupportedOperationException();
        }
    }
}
