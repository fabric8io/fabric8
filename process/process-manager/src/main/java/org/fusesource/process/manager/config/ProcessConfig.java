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

import java.util.List;
import java.util.Map;

/**
 * The configuration DTO stored as JSON so that the system can be restarted and remember how to run & control a managed process
 */
public class ProcessConfig {
    private String name = "<unknown>";
    private String launchScript;
    private String startCommand;
    private String stopCommand;
    private String restartCommand;
    private String statusCommand;
    private String killCommand;
    private String configureCommand;
    private String pidFile;
    private Map<String,String> environment;
    private List<String> installCommands;

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

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public List<String> getInstallCommands() {
        return installCommands;
    }

    public void setInstallCommands(List<String> installCommands) {
        this.installCommands = installCommands;
    }

    public String getConfigureCommand() {
        return configureCommand;
    }

    public void setConfigureCommand(String configureCommand) {
        this.configureCommand = configureCommand;
    }
}
