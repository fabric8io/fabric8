/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.agent;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.IOException;

public class AgentLauncher {

    public static void main(String[] args) {
        if (args.length > 0) {
            try {
                System.err.println("Attaching Fabric8 Agent to process: " + args[0]);
                String options = "";
                for (int i = 1; i < args.length; i++) {
                    options += args[i];
                    if (i < (args.length - 1)) {
                        options += ",";
                    }
                }
                loadAgent(args[0], options);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Usage is " + AgentLauncher.class.getName() + " pid");
        }

    }

    public static void loadAgent(String pid, String args) throws IOException {
        VirtualMachine vm;
        try {
            vm = VirtualMachine.attach(pid);
        } catch (AttachNotSupportedException x) {
            IOException ioe = new IOException(x.getMessage());
            ioe.initCause(x);
            throw ioe;
        }

        try {
            String agent = AgentLauncher.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            System.err.println("Trying to load agent " + agent);
            vm.loadAgent(agent, args);
            System.out.println("Agent successfully loaded");
        } catch (AgentLoadException x) {
            IOException ioe = new IOException(x.getMessage());
            ioe.initCause(x);
            throw ioe;
        } catch (AgentInitializationException x) {
            IOException ioe = new IOException(x.getMessage());
            ioe.initCause(x);
            throw ioe;
        } finally {
            if (vm != null) {
                vm.detach();
            }
        }
    }
}
