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
package org.fusesource.process.manager.support;

import org.fusesource.process.manager.ProcessController;
import org.fusesource.process.manager.commands.Command;
import org.fusesource.process.manager.commands.CommandFailedException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A default implementation of {@link ProcessController} which assumes a launch script which takes opertions as the first argument
 * such as for the <a href="http://refspecs.freestandards.org/LSB_3.1.1/LSB-Core-generic/LSB-Core-generic/iniscrptact.html">Init Script Actions spec</a>
 */
public class DefaultProcessController implements ProcessController
{
    private static Executor defaultExectutor = null;

    private File baseDir = new File(".");
    private Executor executor = null;

    protected static Executor defaultExectutor() {
        if (defaultExectutor == null) {
            defaultExectutor = Executors.newCachedThreadPool();
        }
        return defaultExectutor;
    }

    private String launchScript = "bin/launcher";


    @Override
    public int uninstall() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int start() throws Exception {
        return runCommand("start");
    }

    @Override
    public int stop() throws Exception {
        return runCommand("stop");
    }

    @Override
    public int kill() throws Exception {
        return runCommand("start");
    }

    @Override
    public int restart() throws Exception {
        return runCommand("restart");
    }

    @Override
    public int status() throws Exception {
        return runCommand("status");
    }

    // Properties
    //-------------------------------------------------------------------------
    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public Executor getExecutor() {
        if (executor == null) {
            executor = defaultExectutor();
        }
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public String getLaunchScript() {
        return launchScript;
    }

    public void setLaunchScript(String launchScript) {
        this.launchScript = launchScript;
    }


    // Implementation methods
    //-------------------------------------------------------------------------

    protected int runCommand(String argument) throws IOException, InterruptedException, CommandFailedException {
        Command command = new Command(launchScript, argument).setDirectory(baseDir);
        return command.execute(getExecutor());
    }
}
