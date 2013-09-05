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

import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.fusesource.process.manager.ProcessController;
import org.fusesource.process.manager.config.ProcessConfig;
import org.fusesource.process.manager.support.command.CommandFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
    private static final transient Logger LOG = LoggerFactory.getLogger(DefaultProcessController.class);

    private final int id;
    private final File baseDir;
    private final ProcessConfig config;
    private transient Executor executor;


    public DefaultProcessController(int id, ProcessConfig config, File baseDir) {
        this.id = id;
        this.config = config;
        this.baseDir = baseDir;
    }

    @Override
    public String toString() {
        return "DefaultProcessController(" + id + ")";
    }

    @Override
    public int install() throws InterruptedException, IOException, CommandFailedException {
        int answer = 0;
        List<String> installCommands = config.getInstallCommands();
        if (installCommands != null) {
            for (String installCommand : installCommands) {
                if (!Strings.isNullOrEmpty(installCommand)) {
                    runCommandLine(installCommand);
                }
            }
        }
        return answer;
    }

    @Override
    public int uninstall() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int start() throws Exception {
        return runConfigCommandValueOrLaunchScriptWith(config.getStartCommand(), "start");
    }

    @Override
    public int stop() throws Exception {
        return runConfigCommandValueOrLaunchScriptWith(config.getStopCommand(), "stop");
    }

    @Override
    public int kill() throws Exception {
        String customCommand = config.getKillCommand();
        if (customCommand != null && customCommand.trim().isEmpty()) {
            // lets stop it
            LOG.info("No kill command configured so lets just try stopping " + this);
            return stop();
        }
        return runConfigCommandValueOrLaunchScriptWith(customCommand, "kill");
    }

    @Override
    public int restart() throws Exception {
        String customCommand = config.getRestartCommand();
        if (customCommand != null && customCommand.trim().isEmpty()) {
            // lets stop and start()
            LOG.info("No restart command configured so lets just try stopping " + this + " then starting again.");
            int answer = stop();
            if (answer == 0) {
                answer = start();
            }
            return answer;
        }
        return runConfigCommandValueOrLaunchScriptWith(customCommand, "restart");
    }

    @Override
    public int status() throws Exception {
        return runConfigCommandValueOrLaunchScriptWith(config.getStatusCommand(), "status");
    }

    @Override
    public int configure() throws Exception {
        String customCommand = config.getConfigureCommand();
        if (customCommand != null && customCommand.trim().isEmpty()) {
            // TODO is it ok to simply ignore this?
            LOG.info("No configure command configured " + this);
            return 0;
        }
        return runCommandLine(customCommand);
    }

    // Properties
    //-------------------------------------------------------------------------
    public File getBaseDir() {
        return baseDir;
    }

    public Executor getExecutor() {
    	if (executor == null) {
    	    executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("fuse-process-controller-%s").build());
    	}
        return executor;
    }

    public Integer getPid() throws IOException {
        Integer answer = null;
        String pidFileName = config.getPidFile();
        if (pidFileName != null) {
            File file = new File(baseDir, pidFileName);
            if (file.exists() && file.isFile()) {
                return extractPidFromFile(file);
            }
        }
        File pidFile = new File(baseDir, "var/process.pid");
        if (pidFile.exists()) {
            return extractPidFromFile(pidFile);
        }

        File pidDir = new File(baseDir, "var/run");
        if (pidDir.exists() && pidDir.isDirectory()) {
            String launchScript = getLaunchScript();
            String script = launchScript;
            int idx = script.lastIndexOf("/");
            if (idx < 0) {
                idx = script.lastIndexOf("\\");
            }
            if (idx > 0) {
                script = script.substring(idx + 1);
            }
            // lets try find the file /var/run/launcher.pid by default
            pidFile = new File(pidDir, script + ".pid");
            if (pidFile.exists()) {
                return extractPidFromFile(pidFile);
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

    protected String getLaunchScript() {
        String launchScript = config.getLaunchScript();
        if (launchScript == null) {
            // TODO should we auto-discover here?
            launchScript = "bin/launcher";
        }
        return launchScript;
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

    protected int runConfigCommandValueOrLaunchScriptWith(String command, String launchArgument) throws InterruptedException, IOException, CommandFailedException {
        if (command != null) {
            return runCommandLine(command);
        } else {
            return config.runCommand(getExecutor(), baseDir, getLaunchScript(), launchArgument);
        }
    }

    /**
     * Converts a space separated command line into a Command and executes it
     */
    protected int runCommandLine(String command) throws IOException, InterruptedException, CommandFailedException {
        if (command != null) {
            // TODO warning this doesn't handle quoted strings as a single argument
            List<String> commandArgs = ExecParseUtils.splitToWhiteSpaceSeparatedTokens(command);
            return config.runCommand(getExecutor(), baseDir, commandArgs.toArray(new String[commandArgs.size()]));
        } else {
            return 0;
        }
    }

}
