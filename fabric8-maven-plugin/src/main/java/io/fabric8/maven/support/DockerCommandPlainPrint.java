/*
 * Copyright 2005-2016 Red Hat, Inc.                                    
 *                                                                      
 * Red Hat licenses this file to you under the Apache License, version  
 * 2.0 (the "License"); you may not use this file except in compliance  
 * with the License.  You may obtain a copy of the License at           
 *                                                                      
 *    http://www.apache.org/licenses/LICENSE-2.0                        
 *                                                                      
 * Unless required by applicable law or agreed to in writing, software  
 * distributed under the License is distributed on an "AS IS" BASIS,    
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or      
 * implied.  See the License for the specific language governing        
 * permissions and limitations under the License.
 */
package io.fabric8.maven.support;

import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.VolumeMount;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DockerCommandPlainPrint {

    private static final String DOCKER_PREFIX_LOG_OUTPUT = "docker run -dP ";
    
    private StringBuilder dockerPlainTextCommand;

	public DockerCommandPlainPrint(StringBuilder dockerPlainTextCommand) {
		super();
		this.dockerPlainTextCommand = dockerPlainTextCommand;
		this.dockerPlainTextCommand.append(DOCKER_PREFIX_LOG_OUTPUT);
	}

	public StringBuilder getDockerPlainTextCommand() {
		return dockerPlainTextCommand;
	}

	public void setDockerPlainTextCommand(StringBuilder dockerPlainTextCommand) {
		this.dockerPlainTextCommand = dockerPlainTextCommand;
	}
	
	public void appendParameters(Map<String, String> data, String flag) {
		for (Map.Entry<String, String> entry : data.entrySet())
		{
		    dockerPlainTextCommand.append(flag);
		    dockerPlainTextCommand.append(" ");
		    dockerPlainTextCommand.append(entry.getKey());
		    dockerPlainTextCommand.append("=");
		    dockerPlainTextCommand.append(entry.getValue());
		    dockerPlainTextCommand.append(" ");
		}
	}
	
	public void appendVolumeMounts(List<VolumeMount> volumesMount, String flag) {
		Iterator<VolumeMount> it = volumesMount.iterator();
		while (it.hasNext()){
			VolumeMount vol = it.next();			
			dockerPlainTextCommand.append(flag);
			dockerPlainTextCommand.append(" ");
			dockerPlainTextCommand.append(vol.getMountPath().toString());
			if (vol.getReadOnly()) {
				dockerPlainTextCommand.append(":");
				dockerPlainTextCommand.append("ro");
			}
			dockerPlainTextCommand.append(" ");
		}
	}
	
	public void appendContainerPorts(List<ContainerPort> containerPorts, String flag) {
		Iterator<ContainerPort> it = containerPorts.iterator();
		while (it.hasNext()){
			ContainerPort port = it.next();			
			dockerPlainTextCommand.append(flag);
			dockerPlainTextCommand.append(" ");
            if (port.getHostIP() != null && !port.getHostIP().isEmpty()) {
            	dockerPlainTextCommand.append(port.getHostIP());
            	dockerPlainTextCommand.append(":");
            }
            if (port.getHostPort() != null) {
            	dockerPlainTextCommand.append(port.getHostPort());
            	dockerPlainTextCommand.append(":");
            }
            if (port.getHostIP() != null && !port.getHostIP().isEmpty() && port.getHostPort() == null) {
            	dockerPlainTextCommand.append(":");
            }                       
        	dockerPlainTextCommand.append(port.getContainerPort());
			dockerPlainTextCommand.append(" ");
		}
	}
	
	public void appendImageName(String imageName) {
		dockerPlainTextCommand.append(imageName);
	}
    
}
