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
package io.fabric8.watcher.matchers;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Maven groupId matcher.
 * Syntax:
 *   mvn:groupId:grp1,grp2...
 *
 * Will match directories starting with one of the groupIds.
 */
public class Maven {

    public static final String PREFIX = "mvn:";

    public static final String PREFIX_GROUPID = "groupId:";

    public static PathMatcher parse(String expression) {
        if (expression.startsWith(PREFIX)) {
            String log = expression.substring(PREFIX.length());
            return doParse(log);
        } else {
            throw new IllegalArgumentException("Expression does not start with the required prefix '" + PREFIX + "'");
        }
    }

    private static PathMatcher doParse(String expression) {
        if (expression.startsWith(PREFIX_GROUPID)) {
            String[] groupIds = expression.substring(PREFIX_GROUPID.length()).split(",");
            return new GroupIdPathMatcher(Arrays.asList(groupIds));
        } else {
            throw new IllegalArgumentException("Unrecognized expression: " + expression);
        }
    }

    public static class GroupIdPathMatcher implements PathMatcher {

        Node root = new Node();

        public GroupIdPathMatcher(Collection<String> groupIds) {
            if (groupIds.isEmpty()) {
                root.valid = true;
            } else {
                for (String groupId : groupIds) {
                    addGroupId(groupId);
                }
            }
        }

        private void addGroupId(String groupId) {
            Node node = root;
            for (String path : groupId.split("\\.")) {
                Node child = node.children.get(path);
                if (child == null) {
                    child = new Node();
                    node.children.put(path, child);
                }
                node = child;
            }
            node.valid = true;
        }

        @Override
        public boolean matches(Path path) {
            if (root.valid) {
                return true;
            }
            Node node = root;
            for (Path p : path) {
                node = node.children.get(p.toString());
                if (node == null) {
                    return false;
                } else if (node.valid) {
                    return true;
                }
            }
            return true;
        }

        static class Node {
            boolean valid;
            Map<String, Node> children = new HashMap<String, Node>();
        }
    }

}
