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
package io.fabric8.dosgi.capset;

import java.util.ArrayList;
import java.util.List;

public class SimpleFilter
{
    public static final int MATCH_ALL = 0;
    public static final int AND = 1;
    public static final int OR = 2;
    public static final int NOT = 3;
    public static final int EQ = 4;
    public static final int LTE = 5;
    public static final int GTE = 6;
    public static final int SUBSTRING = 7;
    public static final int PRESENT = 8;
    public static final int APPROX = 9;

    private final String m_name;
    private final Object m_value;
    private final int m_op;
    private String m_string;

    public SimpleFilter(String attr, Object value, int op)
    {
        m_name = attr;
        m_value = value;
        m_op = op;
    }

    public String getName()
    {
        return m_name;
    }

    public Object getValue()
    {
        return m_value;
    }

    public int getOperation()
    {
        return m_op;
    }

    @Override
    public String toString()
    {
        if (m_string == null) {
            switch (m_op)
            {
                case AND:
                    m_string = "(&" + toString((List) m_value) + ")";
                    break;
                case OR:
                    m_string = "(|" + toString((List) m_value) + ")";
                    break;
                case NOT:
                    m_string = "(!" + toString((List) m_value) + ")";
                    break;
                case EQ:
                    m_string = "(" + m_name + "=" + toEncodedString(m_value) + ")";
                    break;
                case LTE:
                    m_string = "(" + m_name + "<=" + toEncodedString(m_value) + ")";
                    break;
                case GTE:
                    m_string = "(" + m_name + ">=" + toEncodedString(m_value) + ")";
                    break;
                case SUBSTRING:
                    m_string = "(" + m_name + "=" + unparseSubstring((List<String>) m_value) + ")";
                    break;
                case PRESENT:
                    m_string = "(" + m_name + "=*)";
                    break;
                case APPROX:
                    m_string = "(" + m_name + "~=" + toEncodedString(m_value) + ")";
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
        return m_string;
    }

    @Override
    public boolean equals(Object that)
    {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        return toString().equals(that.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    private static String toString(List list)
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < list.size(); i++)
        {
            sb.append(list.get(i).toString());
        }
        return sb.toString();
    }

    private static String toDecodedString(String s, int startIdx, int endIdx)
    {
        StringBuffer sb = new StringBuffer(endIdx - startIdx);
        boolean escaped = false;
        for (int i = 0; i < (endIdx - startIdx); i++)
        {
            char c = s.charAt(startIdx + i);
            if (!escaped && (c == '\\'))
            {
                escaped = true;
            }
            else
            {
                escaped = false;
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private static String toEncodedString(Object o)
    {
        if (o instanceof String)
        {
            String s = (String) o;
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < s.length(); i++)
            {
                char c = s.charAt(i);
                if ((c == '\\') || (c == '(') || (c == ')') || (c == '*'))
                {
                    sb.append('\\');
                }
                sb.append(c);
            }

            o = sb.toString();
        }

        return o.toString();
    }

    public static SimpleFilter parse(String filter)
    {
        int idx = skipWhitespace(filter, 0);

        if ((filter == null) || (filter.length() == 0) || (idx >= filter.length()))
        {
            throw new IllegalArgumentException("Null or empty filter.");
        }
        else if (filter.charAt(idx) != '(')
        {
            throw new IllegalArgumentException("Missing opening parenthesis: " + filter);
        }

        SimpleFilter sf = null;
        List stack = new ArrayList();
        boolean isEscaped = false;
        while (idx < filter.length())
        {
            if (sf != null)
            {
                throw new IllegalArgumentException(
                    "Only one top-level operation allowed: " + filter);
            }

            if (!isEscaped && (filter.charAt(idx) == '('))
            {
                // Skip paren and following whitespace.
                idx = skipWhitespace(filter, idx + 1);

                if (filter.charAt(idx) == '&')
                {
                    int peek = skipWhitespace(filter, idx + 1);
                    if (filter.charAt(peek) == '(')
                    {
                        idx = peek - 1;
                        stack.add(0, new SimpleFilter(null, new ArrayList(), SimpleFilter.AND));
                    }
                    else
                    {
                        stack.add(0, idx);
                    }
                }
                else if (filter.charAt(idx) == '|')
                {
                    int peek = skipWhitespace(filter, idx + 1);
                    if (filter.charAt(peek) == '(')
                    {
                        idx = peek - 1;
                        stack.add(0, new SimpleFilter(null, new ArrayList(), SimpleFilter.OR));
                    }
                    else
                    {
                        stack.add(0, idx);
                    }
                }
                else if (filter.charAt(idx) == '!')
                {
                    int peek = skipWhitespace(filter, idx + 1);
                    if (filter.charAt(peek) == '(')
                    {
                        idx = peek - 1;
                        stack.add(0, new SimpleFilter(null, new ArrayList(), SimpleFilter.NOT));
                    }
                    else
                    {
                        stack.add(0, idx);
                    }
                }
                else
                {
                    stack.add(0, idx);
                }
            }
            else if (!isEscaped && (filter.charAt(idx) == ')'))
            {
                Object top = stack.remove(0);
                if (top instanceof SimpleFilter)
                {
                    if (!stack.isEmpty() && (stack.get(0) instanceof SimpleFilter))
                    {
                        ((List) ((SimpleFilter) stack.get(0)).m_value).add(top);
                    }
                    else
                    {
                        sf = (SimpleFilter) top;
                    }
                }
                else if (!stack.isEmpty() && (stack.get(0) instanceof SimpleFilter))
                {
                    ((List) ((SimpleFilter) stack.get(0)).m_value).add(
                        SimpleFilter.subfilter(filter, (Integer) top, idx));
                }
                else
                {
                    sf = SimpleFilter.subfilter(filter, (Integer) top, idx);
                }
            }
            else if (!isEscaped && (filter.charAt(idx) == '\\'))
            {
                isEscaped = true;
            }
            else
            {
                isEscaped = false;
            }

            idx = skipWhitespace(filter, idx + 1);
        }

        if (sf == null)
        {
            throw new IllegalArgumentException("Missing closing parenthesis: " + filter);
        }

        return sf;
    }

    private static SimpleFilter subfilter(String filter, int startIdx, int endIdx)
    {
        final String opChars = "=<>~";

        // Determine the ending index of the attribute name.
        int attrEndIdx = startIdx;
        for (int i = 0; i < (endIdx - startIdx); i++)
        {
            char c = filter.charAt(startIdx + i);
            if (opChars.indexOf(c) >= 0)
            {
                break;
            }
            else if (!Character.isWhitespace(c))
            {
                attrEndIdx = startIdx + i + 1;
            }
        }
        if (attrEndIdx == startIdx)
        {
            throw new IllegalArgumentException(
                "Missing attribute name: " + filter.substring(startIdx, endIdx));
        }
        String attr = filter.substring(startIdx, attrEndIdx);

        // Skip the attribute name and any following whitespace.
        startIdx = skipWhitespace(filter, attrEndIdx);

        // Determine the operator type.
        int op;
        switch (filter.charAt(startIdx))
        {
            case '=':
                op = EQ;
                startIdx++;
                break;
            case '<':
                if (filter.charAt(startIdx + 1) != '=')
                {
                    throw new IllegalArgumentException(
                        "Unknown operator: " + filter.substring(startIdx, endIdx));
                }
                op = LTE;
                startIdx += 2;
                break;
            case '>':
                if (filter.charAt(startIdx + 1) != '=')
                {
                    throw new IllegalArgumentException(
                        "Unknown operator: " + filter.substring(startIdx, endIdx));
                }
                op = GTE;
                startIdx += 2;
                break;
            case '~':
                if (filter.charAt(startIdx + 1) != '=')
                {
                    throw new IllegalArgumentException(
                        "Unknown operator: " + filter.substring(startIdx, endIdx));
                }
                op = APPROX;
                startIdx += 2;
                break;
            default:
                throw new IllegalArgumentException(
                    "Unknown operator: " + filter.substring(startIdx, endIdx));
        }

        // Parse value.
        Object value = toDecodedString(filter, startIdx, endIdx);

        // Check if the equality comparison is actually a substring
        // or present operation.
        if (op == EQ)
        {
            String valueStr = filter.substring(startIdx, endIdx);
            List<String> values = parseSubstring(valueStr);
            if ((values.size() == 2)
                && (values.get(0).length() == 0)
                && (values.get(1).length() == 0))
            {
                op = PRESENT;
            }
            else if (values.size() > 1)
            {
                op = SUBSTRING;
                value = values;
            }
        }

        return new SimpleFilter(attr, value, op);
    }

    public static List<String> parseSubstring(String value)
    {
        List<String> pieces = new ArrayList<String>();
        StringBuffer ss = new StringBuffer();
        // int kind = SIMPLE; // assume until proven otherwise
        boolean wasStar = false; // indicates last piece was a star
        boolean leftstar = false; // track if the initial piece is a star
        boolean rightstar = false; // track if the final piece is a star

        int idx = 0;

        // We assume (sub)strings can contain leading and trailing blanks
        boolean escaped = false;
        for (; ;) {
            if (idx >= value.length()) {
                if (wasStar) {
                    // insert last piece as "" to handle trailing star
                    rightstar = true;
                } else {
                    pieces.add(ss.toString());
                    // accumulate the last piece
                    // note that in the case of
                    // (cn=); this might be
                    // the string "" (!=null)
                }
                ss.setLength(0);
                break;
            }

            // Read the next character and account for escapes.
            char c = value.charAt(idx++);
            if (!escaped && ((c == '(') || (c == ')'))) {
                throw new IllegalArgumentException(
                        "Illegal value: " + value);
            } else if (!escaped && (c == '*')) {
                if (wasStar) {
                    // encountered two successive stars;
                    // I assume this is illegal
                    throw new IllegalArgumentException("Invalid filter string: " + value);
                }
                if (ss.length() > 0) {
                    pieces.add(ss.toString()); // accumulate the pieces
                    // between '*' occurrences
                }
                ss.setLength(0);
                // if this is a leading star, then track it
                if (pieces.size() == 0) {
                    leftstar = true;
                }
                wasStar = true;
            } else if (!escaped && (c == '\\')) {
                escaped = true;
            } else {
                escaped = false;
                wasStar = false;
                ss.append(c);
            }
        }
        if (leftstar || rightstar || pieces.size() > 1)
        {
            // insert leading and/or trailing "" to anchor ends
            if (rightstar)
            {
                pieces.add("");
            }
            if (leftstar)
            {
                pieces.add(0, "");
            }
        }
        return pieces;
    }

    public static String unparseSubstring(List<String> pieces)
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < pieces.size(); i++)
        {
            if (i > 0)
            {
                sb.append("*");
            }
            sb.append(toEncodedString(pieces.get(i)));
        }
        return sb.toString();
    }

    public static boolean compareSubstring(List<String> pieces, String s)
    {
        // Walk the pieces to match the string
        // There are implicit stars between each piece,
        // and the first and last pieces might be "" to anchor the match.
        // assert (pieces.length > 1)
        // minimal case is <string>*<string>

        boolean result = true;
        int len = pieces.size();

        // Special case, if there is only one piece, then
        // we must perform an equality test.
        if (len == 1)
        {
            return s.equals(pieces.get(0));
        }

        // Otherwise, check whether the pieces match
        // the specified string.

        int index = 0;

        for (int i = 0; i < len; i++) {
            String piece = pieces.get(i);

            // If this is the first piece, then make sure the
            // string starts with it.
            if (i == 0) {
                if (!s.startsWith(piece)) {
                    result = false;
                    break;
                }
            }

            // If this is the last piece, then make sure the
            // string ends with it.
            if (i == (len - 1)) {
                result = s.endsWith(piece);
                break;
            }

            // If this is neither the first or last piece, then
            // make sure the string contains it.
            if ((i > 0) && (i < (len - 1))) {
                index = s.indexOf(piece, index);
                if (index < 0) {
                    result = false;
                    break;
                }
            }

            // Move string index beyond the matching piece.
            index += piece.length();
        }

        return result;
    }

    private static int skipWhitespace(String s, int startIdx)
    {
        int len = s.length();
        while ((startIdx < len) && Character.isWhitespace(s.charAt(startIdx)))
        {
            startIdx++;
        }
        return startIdx;
    }
}