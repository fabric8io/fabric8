/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.insight.log;

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
