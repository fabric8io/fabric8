/**
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
package io.fabric8.runtime.container;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.gravia.repository.MavenCoordinates;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.utils.NotNullException;


/**
 * The managed container configuration
 *
 * @since 26-Feb-2014
 */
public abstract class ContainerConfiguration {

    private List<MavenCoordinates> mavenCoordinates = new ArrayList<MavenCoordinates>();
    private File targetDirectory;
    private String javaVmArguments;
    private boolean outputToConsole;
    private boolean mutable = true;

    public abstract RuntimeType getRuntimeType();

    /**
     * Get the array of maven artefacts that are getting unpacked
     * during {@link ManagedContainer#create(ContainerConfiguration)}
     */
    public List<MavenCoordinates> getMavenCoordinates() {
        return Collections.unmodifiableList(mavenCoordinates);
    }

    public void addMavenCoordinates(MavenCoordinates coordinates) {
        assertMutable();
        mavenCoordinates.add(coordinates);
    }

    public File getTargetDirectory() {
        return targetDirectory;
    }

    void setTargetDirectory(File target) {
        assertMutable();
        this.targetDirectory = target;
    }

    public String getJavaVmArguments() {
        return javaVmArguments;
    }

    void setJavaVmArguments(String javaVmArguments) {
        assertMutable();
        this.javaVmArguments = javaVmArguments;
    }

    public boolean isOutputToConsole() {
        return outputToConsole;
    }

    void setOutputToConsole(boolean outputToConsole) {
        this.outputToConsole = outputToConsole;
    }

    protected void validate() {
        NotNullException.assertValue(mavenCoordinates, "mavenCoordinates");
        NotNullException.assertValue(targetDirectory, "targetDirectory");
    }

    void makeImmutable() {
        assertMutable();
        mutable = false;
    }

    private void assertMutable() {
        if (!mutable)
            throw new IllegalStateException("Configuration is immutable");
    }
}
