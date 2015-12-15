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
package io.fabric8.forge.rest.git.dto;

import java.util.Date;

/**
 * Represents information about a commit log or history
 */
public class CommitInfo extends GitDTOSupport {
    private final String sha;
    private final String author;
    private final String name;
    private final String email;
    private final String avatarUrl;
    private final Date date;
    private final boolean merge;
    private final String shortMessage;

    public CommitInfo(String sha, String author, String name, String email, String avatarUrl, Date date, boolean merge, String shortMessage) {
        this.sha = sha;
        this.author = author;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.date = date;
        this.merge = merge;
        this.shortMessage = shortMessage;
    }

    @Override
    public String toString() {
        return "CommitInfo(sha " + sha + " author " + author + " date " + date + " merge " + merge + " " + shortMessage + ")";
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public Date getDate() {
        return date;
    }

    public boolean isMerge() {
        return merge;
    }

    public String getSha() {
        return sha;
    }

    public String getShortMessage() {
        return shortMessage;
    }
}
