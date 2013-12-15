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

/**
 * Logical matchers.
 * Supports parentheses, or, and and not operators.
 */
public class Logical {

    public static final String PREFIX = "log:";

    public static PathMatcher parse(String expression) {
        if (expression.startsWith(PREFIX)) {
            String log = expression.substring(PREFIX.length());
            return doParse(log);
        } else {
            throw new IllegalArgumentException("Expression does not start with the required prefix '" + PREFIX + "'");
        }
    }

    private static PathMatcher doParse(String expression) {
        expression = expression.trim();
        if (!checkParenthesis(expression)) {
            throw new IllegalArgumentException("Not a matching number of '(' and ')' parenthesis: " + expression);
        }

        int index = 0, start = -1, end;
        int[] tokens = new int[32];
        int token = 0;
        for (;;) {
            if (index >= expression.length()) {
                if (start >= 0) {
                    end = index;
                    tokens[token++] = start;
                    tokens[token++] = end;
                }
                break;
            }
            char c = expression.charAt(index);
            if (Character.isWhitespace(c)) {
                if (start >= 0) {
                    end = index;
                    tokens[token++] = start;
                    tokens[token++] = end;
                    start = -1;
                }
                index++;
            } else if (c == '(') {
                if (start >= 0) {
                    end = index;
                    tokens[token++] = start;
                    tokens[token++] = end;
                    start = -1;
                } else {
                    start = index;
                    end = indexOfParenthesisMatch(expression, start);
                    tokens[token++] = start + 1;
                    tokens[token++] = end;
                    start = -1;
                    index = end + 1;
                }
            } else {
                if (start < 0) {
                    start = index;
                }
                index++;
            }
        }
        for (int i = 0; i < token; i+=2) {
            if (expression.regionMatches(tokens[i], "or", 0, tokens[i+1] - tokens[i])) {
                if (i == 0 || i > token - 2) {
                    throw new IllegalArgumentException("Bad syntax: " + expression);
                }
                PathMatcher left = doParse(expression.substring(tokens[0], tokens[i-1]));
                PathMatcher right = doParse(expression.substring(tokens[i+2], tokens[token-1]));
                return new OrMatcher(left, right);
            }
        }
        for (int i = 0; i < token; i+=2) {
            if (expression.regionMatches(tokens[i], "and", 0, tokens[i+1] - tokens[i])) {
                if (i == 0 || i > token - 2) {
                    throw new IllegalArgumentException("Bad syntax: " + expression);
                }
                PathMatcher left = doParse(expression.substring(tokens[0], tokens[i-1]));
                PathMatcher right = doParse(expression.substring(tokens[i+2], tokens[token-1]));
                return new AndMatcher(left, right);
            }
        }
        if (token > 2 && expression.regionMatches(tokens[0], "not", 0, tokens[1] - tokens[0])) {
            PathMatcher right = doParse(expression.substring(tokens[2], tokens[token-1]));
            return new NotMatcher(right);
        }
        if (!expression.matches("[a-z]+:.*")) {
            expression = "glob:" + expression;
        }
        return Matchers.parse(expression);
    }

    /**
     * Examine the supplied string and ensure that all parends appear as matching pairs.
     *
     * @param str
     * 		The target string to examine.
     *
     * @return true if the target string has valid parend pairings.
     */
    static boolean checkParenthesis(String str) {
        boolean result = true;
        if (str != null) {
            int open = 0;
            int closed = 0;

            int i = 0;
            while ((i = str.indexOf('(', i)) >= 0) {
                i++;
                open++;
            }
            i = 0;
            while ((i = str.indexOf(')', i)) >= 0) {
                i++;
                closed++;
            }
            result = open == closed;
        }
        return result;
    }

    /**
     * Given a string and a position in that string of an open parend, find the matching close parend.
     *
     * @param str
     * 		The string to be searched for a matching parend.
     * @param first
     * 		The index in the string of the opening parend whose close value is to be searched.
     *
     * @return the index in the string where the closing parend is located.
     */
    public static int indexOfParenthesisMatch(String str, int first) {
        int index = -1;

        if (first < 0 || first > str.length()) {
            throw new IllegalArgumentException("Invalid position for first parenthesis: " + first);
        }

        if (str.charAt(first) != '(') {
            throw new IllegalArgumentException("character at indicated position is not a parenthesis");
        }

        int depth = 1;
        char[] array = str.toCharArray();
        for (index = first + 1; index < array.length; ++index) {
            char current = array[index];
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                if (--depth == 0) {
                    break;
                }
            }
        }

        if (depth != 0) {
            throw new IllegalArgumentException("Bad syntax: " + str);
        }

        return index;
    }

    static class AndMatcher implements PathMatcher {
        private final PathMatcher left;
        private final PathMatcher right;
        AndMatcher(PathMatcher left, PathMatcher right) {
            this.left = left;
            this.right = right;
        }
        @Override
        public boolean matches(Path path) {
            return left.matches(path) && right.matches(path);
        }
    }

    static class OrMatcher implements PathMatcher {
        private final PathMatcher left;
        private final PathMatcher right;
        OrMatcher(PathMatcher left, PathMatcher right) {
            this.left = left;
            this.right = right;
        }
        @Override
        public boolean matches(Path path) {
            return left.matches(path) || right.matches(path);
        }
    }

    static class NotMatcher implements PathMatcher {
        private final PathMatcher matcher;
        public NotMatcher(PathMatcher matcher) {
            this.matcher = matcher;
        }
        @Override
        public boolean matches(Path path) {
            return !matcher.matches(path);
        }
    }

}
