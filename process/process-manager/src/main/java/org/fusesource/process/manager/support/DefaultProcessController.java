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

import com.google.common.io.Files;
import org.fusesource.process.manager.ProcessController;
import org.fusesource.process.manager.support.command.Command;
import org.fusesource.process.manager.support.command.CommandFailedException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A default implementation of {@link ProcessController} which assumes a launch script which takes opertions as the first argument
 * such as for the <a href="http://refspecs.freestandards.org/LSB_3.1.1/LSB-Core-generic/LSB-Core-generic/iniscrptact.html">Init Script Actions spec</a>
 */
public class DefaultProcessController implements ProcessController
{
    private final File baseDir;
    private final Executor executor;

    private String launchScript = "bin/launcher";

    public DefaultProcessController(Executor executor, File baseDir) {
        this.executor = executor;
        this.baseDir = baseDir;
    }

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

    public Executor getExecutor() {
        return executor;
    }

    public String getLaunchScript() {
        return launchScript;
    }

    public void setLaunchScript(String launchScript) {
        this.launchScript = launchScript;
    }


    public Integer getPid() throws IOException {
        Integer answer = null;
        File pidDir = new File(baseDir, "var/run");
        if (pidDir.exists() && pidDir.isDirectory()) {
            String script = getLaunchScript();
            int idx = script.lastIndexOf("/");
            if (idx < 0) {
                idx = script.lastIndexOf("\\");
            }
            if (idx > 0) {
                script = script.substring(idx + 1);
            }
            // lets try find the file /var/run/launcher.pid by default
            File pidFile = new File(pidDir, script + ".pid");

            if (pidFile.exists()) {
                answer = extractPidFromFile(pidFile);
            }

            // otherwise lets just find a /var/run/*.pid file
            if (answer == null) {
                File[] files = pidDir.listFiles();
                for (File file : files) {
                    if (file.getName().toLowerCase().endsWith(".pid")) {
                        answer = extractPidFromFile(file);
                        if (answer != null) {
                            break;
                        }
                    }
                }
            }
        }
        return answer;
    }

    private Integer extractPidFromFile(File file) throws IOException {
        List<String> lines = Files.readLines(file, Charset.defaultCharset());
        for (String line : lines) {
            String text = line.trim();
            if (text.matches("\\d+")) {
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Failed to parse pid '" + text + "' as a number. Exception: " + e, e);
                }
            }
        }
        return null;
    }

    // Implementation methods
    //-------------------------------------------------------------------------

    protected int runCommand(String argument) throws IOException, InterruptedException, CommandFailedException {
        Command command = new Command(launchScript, argument).setDirectory(baseDir);
        return command.execute(getExecutor());
    }
}
