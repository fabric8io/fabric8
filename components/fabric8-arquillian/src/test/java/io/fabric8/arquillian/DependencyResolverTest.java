/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.arquillian;

import io.fabric8.arquillian.kubernetes.DependencyResolver;
import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.arquillian.kubernetes.log.AnsiLogger;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class DependencyResolverTest {

    @Test
    public void testResolutionOfPomWithNoDeps() throws IOException {
        Session session = new Session("test-session", new AnsiLogger());
        DependencyResolver resolver = new DependencyResolver(DependencyResolver.class.getResource("/test-pom.xml").getFile(), true);
        Assert.assertTrue(resolver.resolve(session).isEmpty());
    }
}