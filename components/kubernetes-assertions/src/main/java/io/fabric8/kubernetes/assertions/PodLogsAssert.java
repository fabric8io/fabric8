/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.kubernetes.assertions;

import io.fabric8.utils.IOHelpers;
import org.assertj.core.api.Fail;
import org.assertj.core.api.MapAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Collect the logs from a number of pods so that they can be asserted on
 */
public class PodLogsAssert extends MapAssert<String, String> {
    private static final transient Logger LOG = LoggerFactory.getLogger(PodLogsAssert.class);

    private final String containerName;

    public PodLogsAssert(Map<String, String> actual, String containerName) {
        super(actual);
        this.containerName = containerName;
        writeLogs();
    }

    public void doesNotContainText(String... texts) {
        for (String text : texts) {
            Set<Map.Entry<String, String>> entries = actual.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                String podName = entry.getKey();
                String value = entry.getValue();
                File file = podLogFileName(podName);
                int idx = value.indexOf(text);
                if (idx >= 0) {
                    Fail.fail("Log of pod " + podName + " in file: " + file + " contains text `" + text + "` at " + textCoords(value.substring(0, idx)));
                } else {
                    LOG.debug("does not contain '" + text + "' in  Log of pod " + podName + " in file: " + file);
                }
            }
        }
    }

    public void doesNotContainTextCount(int count, String... texts) {
        if (count == 1) {
            doesNotContainText(texts);
        }
        for (String text : texts) {
            Set<Map.Entry<String, String>> entries = actual.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                String podName = entry.getKey();
                String value = entry.getValue();
                File file = podLogFileName(podName);
                int idx = 0;
                for (int i = 0; idx >= 0 && i < count; i++) {
                    int next = value.indexOf(text, idx);
                    if (next >= 0) {
                        idx = next + 1;
                    } else {
                        idx = next;
                    }
                }
                if (idx >= 0) {
                    Fail.fail("Log of pod " + podName + " in file: " + file + " contains text `" + text + "` " + count + " times with the last at at " + textCoords(value.substring(0, idx - 1)));
                } else {
                    LOG.debug("does not contain '" + text + "' in Log of pod " + podName + " in file: " + file + " " + count + " times");
                }
            }
        }
    }

    /**
     * Returns the line number and column of the text
     */
    public static String textCoords(String text) {
        int line = 1;
        int idx = 0;
        while (true) {
            int next =text.indexOf('\n', idx);
            if (next < 0) {
                break;
            }
            idx = next + 1;
            line += 1;
        }
        int column = 1 + text.length() - idx;
        return "" + line + ":" + column;
    }

    protected File podLogFileName(String podName) {
        // lets return the file we use to store the pod logs
        String basedir = System.getProperty("basedir", ".");
        File dir = new File(basedir, "target/fabric8/systest/logs");
        String name = podName;
        if (containerName != null) {
            name += "." + containerName;
        }
        name += ".log";
        File answer = new File(dir, name);
        answer.getParentFile().mkdirs();
        return answer;
    }

    private void writeLogs() {
        Set<Map.Entry<String, String>> entries = actual.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String podName = entry.getKey();
            String value = entry.getValue();
            File file = podLogFileName(podName);
            try {
                IOHelpers.writeFully(file, value);
            } catch (IOException e) {
                LOG.error("Failed to write log of pod " + podName + " container:" + containerName + " to file: "+ file + ". " + e, e);
            }
        }
    }
}
