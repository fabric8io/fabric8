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
package io.fabric8.maven.support;

import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.VolumeMount;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the DockerCommandPlainPrint class
 */
public class DockerCommandPlainPrintTest {

    @Test
    public void testDockerCommandPlainPrintTest() throws Exception {
        Map<String,String> env = new LinkedHashMap<String,String>();
        env.put("FOO", "bar");
        env.put("USER", "test");
        env.put("PWD", "pass");
        
        StringBuilder sb = new StringBuilder();
        DockerCommandPlainPrint plainPrint = new DockerCommandPlainPrint(sb);
        plainPrint.appendParameters(env, IDockerCommandPlainPrintCostants.EXPRESSION_FLAG);
        plainPrint.appendImageName("test/test_image");
        
        String expected = "docker run -dP -e FOO=bar -e USER=test -e PWD=pass test/test_image";
        
        assertThat(plainPrint.getDockerPlainTextCommand().toString()).isEqualTo(expected);

	}
    
    @Test
    public void testDockerCommandPlainPrintWithVolumeTest() throws Exception {
        Map<String,String> env = new LinkedHashMap<String,String>();
        env.put("FOO", "bar");
        env.put("USER", "test");
        env.put("PWD", "pass");
        
        List<VolumeMount> vmList = new ArrayList<VolumeMount>();
        
        VolumeMount vm = new VolumeMount();
        vm.setName("test");
        vm.setMountPath("/var/testtest/");
        vm.setReadOnly(true);
        vmList.add(vm);
        
        StringBuilder sb = new StringBuilder();
        DockerCommandPlainPrint plainPrint = new DockerCommandPlainPrint(sb);
        plainPrint.appendParameters(env, IDockerCommandPlainPrintCostants.EXPRESSION_FLAG);
        plainPrint.appendVolumeMounts(vmList, IDockerCommandPlainPrintCostants.VOLUME_FLAG);
        plainPrint.appendImageName("test/test_image");
        
        String expected = "docker run -dP -e FOO=bar -e USER=test -e PWD=pass -v /var/testtest/:ro test/test_image";
        
        assertThat(plainPrint.getDockerPlainTextCommand().toString()).isEqualTo(expected);
    }
    
    @Test
    public void testDockerCommandPlainPrintWithVolumeAndPortTest() throws Exception {
        Map<String,String> env = new LinkedHashMap<String,String>();
        env.put("FOO", "bar");
        env.put("USER", "test");
        env.put("PWD", "pass");
        
        List<VolumeMount> vmList = new ArrayList<VolumeMount>();
        
        VolumeMount vm = new VolumeMount();
        vm.setName("test");
        vm.setMountPath("/var/testtest/");
        vm.setReadOnly(true);
        vmList.add(vm);
        
        List<ContainerPort> contPort = new ArrayList<ContainerPort>();
        ContainerPort p = new ContainerPort();
        p.setHostIP("192.168.1.1");
        p.setHostPort(32768);
        p.setContainerPort(8080);       
        contPort.add(p);
        
        ContainerPort p1 = new ContainerPort();
        p1.setHostIP("192.168.1.1");
        p1.setContainerPort(8081);       
        contPort.add(p1);
        
        ContainerPort p2 = new ContainerPort();
        p2.setHostIP("");
        p2.setHostPort(32770);
        p2.setContainerPort(8082);       
        contPort.add(p2);
        
        ContainerPort p3 = new ContainerPort();
        p3.setHostIP("");
        p3.setContainerPort(8083);       
        contPort.add(p3);
        
        StringBuilder sb = new StringBuilder();
        DockerCommandPlainPrint plainPrint = new DockerCommandPlainPrint(sb);
        plainPrint.appendParameters(env, IDockerCommandPlainPrintCostants.EXPRESSION_FLAG);
        plainPrint.appendContainerPorts(contPort, IDockerCommandPlainPrintCostants.PORT_FLAG);
        plainPrint.appendVolumeMounts(vmList, IDockerCommandPlainPrintCostants.VOLUME_FLAG);
        plainPrint.appendImageName("test/test_image");
        
        String expected = "docker run -dP -e FOO=bar -e USER=test -e PWD=pass -p 192.168.1.1:32768:8080 -p 192.168.1.1::8081 -p 32770:8082 -p 8083 -v /var/testtest/:ro test/test_image";
        
        assertThat(plainPrint.getDockerPlainTextCommand().toString()).isEqualTo(expected);
    }
}
