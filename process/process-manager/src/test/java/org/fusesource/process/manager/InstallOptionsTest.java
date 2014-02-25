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
package org.fusesource.process.manager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.fusesource.process.manager.InstallOptions.InstallOptionsBuilder;

public class InstallOptionsTest extends Assert {

    InstallOptions options;

    @Before
    public void setUp() throws MalformedURLException {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");
        options = new InstallOptionsBuilder().
                groupId("org.apache.camel").artifactId("camel-core").version("2.12.0").
                optionalDependencyPatterns(null).
                excludeDependencyFilterPatterns(null).
                build();
    }

    @Test
    public void shouldBuildParametersUrl() throws MalformedURLException {
        assertNotNull(options.getUrl());
        assertEquals(new URL("mvn:org.apache.camel/camel-core/2.12.0/jar"), options.getUrl());
    }

    @Test
    public void shouldSetEmptyOptionalDependencyPatterns() {
        assertNotNull(options.getOptionalDependencyPatterns());
        assertEquals(0, options.getOptionalDependencyPatterns().length);
    }

    @Test
    public void shouldSetEmptyExcludeDependencyFilterPatterns() {
        assertNotNull(options.getExcludeDependencyFilterPatterns());
        assertEquals(0, options.getExcludeDependencyFilterPatterns().length);
    }

}