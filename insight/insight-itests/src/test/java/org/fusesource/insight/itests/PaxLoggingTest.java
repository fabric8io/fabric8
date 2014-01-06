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
package org.fusesource.insight.itests;

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import io.fabric8.itests.paxexam.support.FabricTestSupport;
import org.fusesource.insight.log.LogEvent;
import org.fusesource.insight.log.LogFilter;
import org.fusesource.insight.log.LogResults;
import org.fusesource.insight.log.service.LogQuery;
import org.fusesource.insight.log.service.LogQueryMBean;
import org.fusesource.insight.log.support.Predicate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class PaxLoggingTest extends FabricTestSupport {

    String info1 = "InfoOne message";
    String info2 = "InfoTwo message";
    String debug1 = "DebugOne message";
    String debug2 = "DebugOne message";
    String warn1 = "WarnOne message";
    String warn2 = "WarnTwo message";
    String error1 = "ErrorOne message";
    String error2 = "ErrorTwo message";

    @Test
    public void testQueryOfLogMessages() throws Exception {

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        LogQueryMBean logQuery = JMX.newMBeanProxy(mbs, new ObjectName("org.fusesource.insight:type=LogQuery"), LogQueryMBean.class);


        // lets wait until the last moment to create the logger so we've time to configure the appender
        Logger LOG = LoggerFactory.getLogger(PaxLoggingTest.class);

        LOG.info(info1);
        LOG.debug(debug1);
        LOG.warn(warn1);
        LOG.error(error1);
        LOG.info(info2);
        LOG.debug(debug2);
        LOG.warn(warn2);
        LOG.error(error2);

        // now lets lookup the MBean

        assertLogQuery(logQuery);
    }

    protected void assertLogQuery(LogQueryMBean mBean) throws Exception {
        LogResults results = mBean.allLogResults();
        List<LogEvent> logEvents = assertNotEmpty(results);

        assertMatches(logEvents, messagePredicate("INFO", info1));
        assertMatches(logEvents, messagePredicate("INFO", info2));
        assertMatches(logEvents, messagePredicate("ERROR", error1));
        assertMatches(logEvents, messagePredicate("ERROR", error2));

        // test a filter
        LogFilter filter = new LogFilter();
        filter.setLevels(new String[] {"ERROR"});
        logEvents = assertNotEmpty(mBean.queryLogResults(filter));
        assertMatches(logEvents, messagePredicate("ERROR", error1));
        assertMatches(logEvents, messagePredicate("ERROR", error2));
        assertNotMatches(logEvents, messagePredicate("INFO", info1));
        assertNotMatches(logEvents, messagePredicate("INFO", info2));
    }

    public static Predicate<LogEvent> messagePredicate(final String level, final String message) {
        return new Predicate<LogEvent>() {
            public boolean matches(LogEvent logEvent) {
                return level.equals(logEvent.getLevel()) && message.equals(logEvent.getMessage());
            }

            public String toString() {
                return "MessagePredicate(level: '" + level + "' message: " + message + "')";
            }
        };
    }

    public static List<LogEvent> assertNotEmpty(LogResults results) {
        assertNotNull("Should have a LogResults", results);
        List<LogEvent> events = results.getEvents();
        assertNotNull("Should have events", events);

        assertTrue("Events.size() should not be zero!", events.size() > 0);

        Long fromTimestamp = results.getFromTimestamp();
        Long toTimestamp = results.getToTimestamp();
        assertNotNull("Should have a fromTimestamp", fromTimestamp);
        assertNotNull("Should have a toTimestamp", toTimestamp);
        return events;
    }

    public static <T> void assertMatches(List<T> logEvents, Predicate<T> predicate) {
        StringBuffer buffer = new StringBuffer();
        for (T element : logEvents) {
            if (predicate.matches(element)) {
                return;
            }
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append(element);
        }

        fail("Could not find message matching " + predicate + " when found: " + buffer);
    }

    public static <T> void assertNotMatches(List<T> logEvents, Predicate<T> predicate) {
        for (T element : logEvents) {
            if (predicate.matches(element)) {
                fail("Found element " + element + " which should not match predicatE: " + predicate);
            }
        }
    }


    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //KarafDistributionOption.debugConfiguration("5005", true)
        };
    }

}
