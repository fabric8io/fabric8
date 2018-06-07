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

package io.fabric8.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class ZipsTest {
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test(expected = IOException.class)
    public void unzip() throws Exception {
        File zipFile = new File(getClass().getResource("/zip-test.zip").getFile());
        File outDir = tempDir.newFolder("test").getAbsoluteFile();
        Zips.unzip(new FileInputStream(zipFile),outDir);
    }

}