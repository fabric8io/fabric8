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
package io.fabric8.support.api;

import io.fabric8.common.util.IOHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by gertv on 6/24/14.
 */
public class FileResource implements Resource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileResource.class);

    private final File file;

    public FileResource(File file) {
        super();
        this.file = file;
    }

    public String getName() {
        return file.getName();
    }

    public void write(OutputStream os) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            IOHelpers.writeTo(os, fis, false);
        } catch (IOException e) {
            LOGGER.warn(String.format("Error while adding content of file %s to the support information", file.getName()), e);
        } finally {
            IOHelpers.close(fis);
        }

    }
}
