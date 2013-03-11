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
package org.fusesource.insight.log.log4j;

import java.util.Arrays;
import java.util.List;

import org.fusesource.insight.log.LogEvent;
import org.fusesource.insight.log.LogFilter;
import org.fusesource.insight.log.LogResults;
import org.fusesource.insight.log.support.LogQuerySupportMBean;
import org.fusesource.insight.log.support.Predicate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.util.CamelContextHelper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ExceptionMavenCoordsTest {
    private transient Logger LOG = LoggerFactory.getLogger(ExceptionMavenCoordsTest.class);
    protected Log4jLogQuery logQuery = new Log4jLogQuery();

    @Test
    public void testQueryOfLogMessages() throws Exception {
        Logger testLog = LoggerFactory.getLogger("org.fusesource.insight.log.log4j.TestLogger");

        // now lets force an exception with a stack trace from camel...
        try {
            CamelContextHelper.getMandatoryEndpoint(null, null);
        } catch (Throwable e) {
            testLog.error("Expected exception for testing: " + e, e);
        }

        // now lets find the error
        LogResults results = logQuery.allLogResults();
        List<LogEvent> logEvents = Log4jTest.assertNotEmpty(results);
        LogEvent log = logEvents.get(0);
        assertNotNull("Should have a log event", log);

        List<String> list = Arrays.asList(log.getException());
        assertTrue("Should have more than 1 items in the stack trace but got: " + list, list.size() > 1);
        String first = list.get(1);
        LOG.info("First line: " + first);
        String expects = "[org.apache.camel:camel-core:";

        assertTrue("Should have camel coordinate '" + expects + "' but got " + first,
                first.indexOf(expects) > 0);

        for (String line : list) {
            LOG.info(line);
        }
    }



    @Before
    public void start() {
        logQuery.start();
    }

    @After
    public void stop() {
        logQuery.stop();
    }
}
