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
package io.fabric8.insight.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 */
public class LogFilter {
    private int count;
    private String[] levels;
    private String matchesText;
    private Long beforeTimestamp;
    private Long afterTimestamp;

    @Override
    public String toString() {
        return "LogFilter{" +
                "count=" + count +
                ", afterTimestamp=" + afterTimestamp +
                ", matchesText='" + matchesText + '\'' +
                '}';
    }

    public Set<String> getLevelsSet() {
        if (levels == null || levels.length == 0) {
            return Collections.EMPTY_SET;
        }
        return new HashSet<String>(Arrays.asList(levels));
    }


    // Properties
    //-------------------------------------------------------------------------

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String[] getLevels() {
        return levels;
    }

    public void setLevels(String[] levels) {
        this.levels = levels;
    }

    public String getMatchesText() {
        return matchesText;
    }

    public void setMatchesText(String matchesText) {
        this.matchesText = matchesText;
    }

    public Long getBeforeTimestamp() {
        return beforeTimestamp;
    }

    public Long getAfterTimestamp() {
        return afterTimestamp;
    }

    public void setAfterTimestamp(Long afterTimestamp) {
        this.afterTimestamp = afterTimestamp;
    }

    public void setBeforeTimestamp(Long beforeTimestamp) {
        this.beforeTimestamp = beforeTimestamp;
    }
}
