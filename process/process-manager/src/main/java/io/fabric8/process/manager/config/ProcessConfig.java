/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.process.manager.config;

import io.fabric8.process.manager.support.command.Command;
import io.fabric8.process.manager.support.command.CommandFailedException;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    private Map<String,String> environment = new TreeMap<String, String>();
    private List<String> installCommands = new ArrayList<String>();

    private String deployPath;
    private String sharedLibraryPath;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProcessConfig that = (ProcessConfig) o;

        if (configureCommand != null ? !configureCommand.equals(that.configureCommand) : that.configureCommand != null)
            return false;
        if (deployPath != null ? !deployPath.equals(that.deployPath) : that.deployPath != null) return false;
        if (environment != null ? !environment.equals(that.environment) : that.environment != null) return false;
        if (installCommands != null ? !installCommands.equals(that.installCommands) : that.installCommands != null)
            return false;
        if (killCommand != null ? !killCommand.equals(that.killCommand) : that.killCommand != null) return false;
        if (launchScript != null ? !launchScript.equals(that.launchScript) : that.launchScript != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (pidFile != null ? !pidFile.equals(that.pidFile) : that.pidFile != null) return false;
        if (restartCommand != null ? !restartCommand.equals(that.restartCommand) : that.restartCommand != null)
            return false;
        if (sharedLibraryPath != null ? !sharedLibraryPath.equals(that.sharedLibraryPath) : that.sharedLibraryPath != null)
            return false;
        if (startCommand != null ? !startCommand.equals(that.startCommand) : that.startCommand != null) return false;
        if (statusCommand != null ? !statusCommand.equals(that.statusCommand) : that.statusCommand != null)
            return false;
        if (stopCommand != null ? !stopCommand.equals(that.stopCommand) : that.stopCommand != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (launchScript != null ? launchScript.hashCode() : 0);
        result = 31 * result + (startCommand != null ? startCommand.hashCode() : 0);
        result = 31 * result + (stopCommand != null ? stopCommand.hashCode() : 0);
        result = 31 * result + (restartCommand != null ? restartCommand.hashCode() : 0);
        result = 31 * result + (statusCommand != null ? statusCommand.hashCode() : 0);
        result = 31 * result + (killCommand != null ? killCommand.hashCode() : 0);
        result = 31 * result + (configureCommand != null ? configureCommand.hashCode() : 0);
        result = 31 * result + (pidFile != null ? pidFile.hashCode() : 0);
        result = 31 * result + (environment != null ? environment.hashCode() : 0);
        result = 31 * result + (installCommands != null ? installCommands.hashCode() : 0);
        result = 31 * result + (deployPath != null ? deployPath.hashCode() : 0);
        result = 31 * result + (sharedLibraryPath != null ? sharedLibraryPath.hashCode() : 0);
        return result;
    }

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

    public void setEnvironment(Map<String, String> environment) {
        this.environment = new TreeMap<String, String>(environment);
    }

    public void setInstallCommands(List<String> installCommands) {
        this.installCommands = installCommands;
    }

    public int runCommand(Executor executor, File baseDir, String... arguments) throws IOException, InterruptedException, CommandFailedException {
        // ignore empty commands
        if (arguments == null || arguments.length == 0) {
            return 0;
        }
        Command command = new Command(arguments).setDirectory(baseDir);
        Map<String,String> environment = getEnvironment();
        if (environment != null && environment.size() > 0) {
            command = command.addEnvironment(environment);
        }
        return command.execute(executor);
    }
}
