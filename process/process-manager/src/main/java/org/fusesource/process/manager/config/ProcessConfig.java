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
package org.fusesource.process.manager.config;

import org.fusesource.process.manager.support.command.Command;
import org.fusesource.process.manager.support.command.CommandFailedException;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * The configuration DTO stored as JSON so that the system can be restarted and remember how to run & control a managed process
 */
public class ProcessConfig implements Serializable {

    private static final long serialVersionUID = -2472076539312397232L;

    private String name = "<unknown>";
    private String launchScript;
    private String startCommand;
    private String stopCommand;
    private String restartCommand;
    private String statusCommand;
    private String killCommand;
    private String configureCommand;
    private String pidFile;
    private final Map<String,String> environment = new HashMap<String, String>();
    private final List<String> installCommands = new ArrayList<String>();

    private String deployPath;
    private String sharedLibraryPath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKillCommand() {
        return killCommand;
    }

    public void setKillCommand(String killCommand) {
        this.killCommand = killCommand;
    }

    public String getLaunchScript() {
        return launchScript;
    }

    public void setLaunchScript(String launchScript) {
        this.launchScript = launchScript;
    }

    public String getPidFile() {
        return pidFile;
    }

    public void setPidFile(String pidFile) {
        this.pidFile = pidFile;
    }

    public String getRestartCommand() {
        return restartCommand;
    }

    public void setRestartCommand(String restartCommand) {
        this.restartCommand = restartCommand;
    }

    public String getStartCommand() {
        return startCommand;
    }

    public void setStartCommand(String startCommand) {
        this.startCommand = startCommand;
    }

    public String getStatusCommand() {
        return statusCommand;
    }

    public void setStatusCommand(String statusCommand) {
        this.statusCommand = statusCommand;
    }

    public String getStopCommand() {
        return stopCommand;
    }

    public void setStopCommand(String stopCommand) {
        this.stopCommand = stopCommand;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public List<String> getInstallCommands() {
        return installCommands;
    }

    public String getConfigureCommand() {
        return configureCommand;
    }

    public void setConfigureCommand(String configureCommand) {
        this.configureCommand = configureCommand;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }

    public String getSharedLibraryPath() {
        return sharedLibraryPath;
    }

    public void setSharedLibraryPath(String sharedLibraryPath) {
        this.sharedLibraryPath = sharedLibraryPath;
    }

    public int runCommand(Executor executor, File baseDir, String... arguments) throws IOException, InterruptedException, CommandFailedException {
        Command command = new Command(arguments).setDirectory(baseDir);
        Map<String,String> environment = getEnvironment();
        if (environment != null && environment.size() > 0) {
            command = command.addEnvironment(environment);
        }
        return command.execute(executor);
    }
}
