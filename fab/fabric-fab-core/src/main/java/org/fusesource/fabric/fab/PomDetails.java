/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

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

    public Model getModel() throws IOException, XmlPullParserException {
        Model model = new MavenXpp3Reader().read(new FileInputStream(file));
        model.setGroupId(properties.getProperty("groupId", model.getGroupId()));
        model.setArtifactId(properties.getProperty("artifactId", model.getArtifactId()));
        model.setVersion(properties.getProperty("version", model.getVersion()));
        return model;
    }
}
