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
package io.fabric8.kubernetes.api;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class ParseDateTimeTest {

    private static final transient Logger LOG = LoggerFactory.getLogger(ParseDateTimeTest.class);

    @Test
    public void testParseDateTime() throws Exception {
        Date date = KubernetesHelper.parseDate("2015-03-26T17:11:55Z");
        assertThat(date).isNotNull();
        System.out.println("Parsed date: " + date);
    }
}
