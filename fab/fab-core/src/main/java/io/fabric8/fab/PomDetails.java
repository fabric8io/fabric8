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
package io.fabric8.fab;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Represents the pom file and properties inside a jar
 */
public class PomDetails {
    private final File file;
    private final Properties properties;

    public PomDetails(File file, Properties properties) {
        this.file = file;
        this.properties = properties;
    }

    /**
     * Returns true if the pom file exists and the properties are not null
     */
    public boolean isValid() {
        return file != null && properties != null && file.exists();
    }

    @Override
    public String toString() {
        return "PomDetails[" + file + "; properties: " + properties + "]";
    }

    public File getFile() {
        return file;
    }

    public Properties getProperties() {
        return properties;
    }

    public Model getModel() throws IOException {
        try {
            Model model = new MavenXpp3Reader().read(new FileInputStream(file));
            model.setGroupId(properties.getProperty("groupId", model.getGroupId()));
            model.setArtifactId(properties.getProperty("artifactId", model.getArtifactId()));
            model.setVersion(properties.getProperty("version", model.getVersion()));
            return model;
        } catch (XmlPullParserException e) {
            throw new IOException("Error parsing maven pom " + file, e);
        }
    }
}
