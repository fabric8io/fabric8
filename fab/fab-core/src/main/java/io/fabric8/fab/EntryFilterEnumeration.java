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
package io.fabric8.fab;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class EntryFilterEnumeration implements Enumeration
{
    private final List<Iterator<String>> m_enumerations;
    private final List<Content> m_jars;
    private int m_moduleIndex = 0;
    private final String m_path;
    private final List<String> m_filePattern;
    private final boolean m_recurse;
    private final boolean m_isURLValues;
    private final Set<String> m_dirEntries = new HashSet();
    private final List<Object> m_nextEntries = new ArrayList(2);

    public EntryFilterEnumeration(
        List<Content> jars, String path,
        String filePattern, boolean recurse, boolean isURLValues)
    {
        m_jars = jars;
        m_enumerations = new ArrayList(m_jars.size());
        for (int i = 0; i < m_jars.size(); i++)
        {
            m_enumerations.add(m_jars.get(i).getEntries().iterator());
        }
        m_recurse = recurse;
        m_isURLValues = isURLValues;

        // Sanity check the parameters.
        if (path == null)
        {
            throw new IllegalArgumentException("The path for findEntries() cannot be null.");
        }
        // Strip leading '/' if present.
        if ((path.length() > 0) && (path.charAt(0) == '/'))
        {
            path = path.substring(1);
        }
        // Add a '/' to the end if not present.
        if ((path.length() > 0) && (path.charAt(path.length() - 1) != '/'))
        {
            path = path + "/";
        }
        m_path = path;

        // File pattern defaults to "*" if not specified.
        filePattern = (filePattern == null) ? "*" : filePattern;

        m_filePattern = parseSubstring(filePattern);

        findNext();
    }

    public synchronized boolean hasMoreElements()
    {
        return (!m_nextEntries.isEmpty());
    }

    public synchronized Object nextElement()
    {
        if (m_nextEntries.isEmpty())
        {
            throw new NoSuchElementException("No more entries.");
        }
        Object last = m_nextEntries.remove(0);
        findNext();
        return last;
    }

    private void findNext()
    {
        // This method filters the content entry enumeration, such that
        // it only displays the contents of the directory specified by
        // the path argument either recursively or not; much like using
        // "ls -R" or "ls" to list the contents of a directory, respectively.
        if (m_enumerations == null)
        {
            return;
        }
        while ((m_moduleIndex < m_enumerations.size()) && m_nextEntries.isEmpty())
        {
            while (m_enumerations.get(m_moduleIndex) != null
                && m_enumerations.get(m_moduleIndex).hasNext()
                && m_nextEntries.isEmpty())
            {
                // Get the current entry to determine if it should be filtered or not.
                String entryName = m_enumerations.get(m_moduleIndex).next();
                // Check to see if the current entry is a descendent of the specified path.
                if (!entryName.equals(m_path) && entryName.startsWith(m_path))
                {
                    // Cached entry URL. If we are returning URLs, we use this
                    // cached URL to avoid doing multiple URL lookups from a module
                    // when synthesizing directory URLs.
                    URL entryURL = null;

                    // If the current entry is in a subdirectory of the specified path,
                    // get the index of the slash character.
                    int dirSlashIdx = entryName.indexOf('/', m_path.length());

                    // JAR files are supposed to contain entries for directories,
                    // but not all do. So calculate the directory for this entry
                    // and see if we've already seen an entry for the directory.
                    // If not, synthesize an entry for the directory. If we are
                    // doing a recursive match, we need to synthesize each matching
                    // subdirectory of the entry.
                    if (dirSlashIdx >= 0)
                    {
                        // Start synthesizing directories for the current entry
                        // at the subdirectory after the initial path.
                        int subDirSlashIdx = dirSlashIdx;
                        String dir;
                        do
                        {
                            // Calculate the subdirectory name.
                            dir = entryName.substring(0, subDirSlashIdx + 1);
                            // If we have not seen this directory before, then record
                            // it and potentially synthesize an entry for it.
                            if (!m_dirEntries.contains(dir))
                            {
                                // Record it.
                                m_dirEntries.add(dir);
                                // If the entry is actually a directory entry (i.e.,
                                // it ends with a slash), then we don't need to
                                // synthesize an entry since it exists; otherwise,
                                // synthesize an entry if it matches the file pattern.
                                if (entryName.length() != (subDirSlashIdx + 1))
                                {
                                    // See if the file pattern matches the last
                                    // element of the path.
                                    if (compareSubstring(m_filePattern, getLastPathElement(dir)))
                                    {
                                        if (m_isURLValues)
                                        {
                                            try
                                            {
                                                entryURL = (entryURL == null)
                                                    ? m_jars.get(m_moduleIndex).getURL(entryName)
                                                    : entryURL;
                                                m_nextEntries.add(new URL(entryURL, "/" + dir));
                                            }
                                            catch (MalformedURLException ex)
                                            {
                                            }
                                        }
                                        else
                                        {
                                            m_nextEntries.add(dir);
                                        }
                                    }
                                }
                            }
                            // Now prepare to synthesize the next subdirectory
                            // if we are matching recursively.
                            subDirSlashIdx = entryName.indexOf('/', dir.length());
                        }
                        while (m_recurse && (subDirSlashIdx >= 0));
                    }

                    // Now we actually need to check if the current entry itself should
                    // be filtered or not. If we are recursive or the current entry
                    // is a child (not a grandchild) of the initial path, then we need
                    // to check if it matches the file pattern.
                    if (m_recurse || (dirSlashIdx < 0) || (dirSlashIdx == entryName.length() - 1))
                    {
                        // See if the file pattern matches the last element of the path.
                        if (compareSubstring(m_filePattern, getLastPathElement(entryName)))
                        {
                            if (m_isURLValues)
                            {
                                try
                                {
                                    entryURL = (entryURL == null)
                                        ? m_jars.get(m_moduleIndex).getURL(entryName)
                                        : entryURL;
                                    m_nextEntries.add(entryURL);
                                }
                                catch (MalformedURLException ex)
                                {
                                }
                            }
                            else
                            {
                                m_nextEntries.add(entryName);
                            }
                        }
                    }
                }
            }
            if (m_nextEntries.isEmpty())
            {
                m_moduleIndex++;
            }
        }
    }

    private static String getLastPathElement(String entryName)
    {
        int endIdx = (entryName.charAt(entryName.length() - 1) == '/')
            ? entryName.length() - 1
            : entryName.length();
        int startIdx = (entryName.charAt(entryName.length() - 1) == '/')
            ? entryName.lastIndexOf('/', endIdx - 1) + 1
            : entryName.lastIndexOf('/', endIdx) + 1;
        return entryName.substring(startIdx, endIdx);
    }

    static List<String> parseSubstring(String value)
    {
        List<String> pieces = new ArrayList();
        StringBuffer ss = new StringBuffer();
        // int kind = SIMPLE; // assume until proven otherwise
        boolean wasStar = false; // indicates last piece was a star
        boolean leftstar = false; // track if the initial piece is a star
        boolean rightstar = false; // track if the final piece is a star

        int idx = 0;

        // We assume (sub)strings can contain leading and trailing blanks
        boolean escaped = false;
loop:   for (;;)
        {
            if (idx >= value.length())
            {
                if (wasStar)
                {
                    // insert last piece as "" to handle trailing star
                    rightstar = true;
                }
                else
                {
                    pieces.add(ss.toString());
                    // accumulate the last piece
                    // note that in the case of
                    // (cn=); this might be
                    // the string "" (!=null)
                }
                ss.setLength(0);
                break loop;
            }

            // Read the next character and account for escapes.
            char c = value.charAt(idx++);
            if (!escaped && ((c == '(') || (c == ')')))
            {
                throw new IllegalArgumentException(
                    "Illegal value: " + value);
            }
            else if (!escaped && (c == '*'))
            {
                if (wasStar)
                {
                    // encountered two successive stars;
                    // I assume this is illegal
                    throw new IllegalArgumentException("Invalid filter string: " + value);
                }
                if (ss.length() > 0)
                {
                    pieces.add(ss.toString()); // accumulate the pieces
                    // between '*' occurrences
                }
                ss.setLength(0);
                // if this is a leading star, then track it
                if (pieces.size() == 0)
                {
                    leftstar = true;
                }
                wasStar = true;
            }
            else if (!escaped && (c == '\\'))
            {
                escaped = true;
            }
            else
            {
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

    static boolean compareSubstring(List<String> pieces, String s)
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

loop:   for (int i = 0; i < len; i++)
        {
            String piece = pieces.get(i);

            // If this is the first piece, then make sure the
            // string starts with it.
            if (i == 0)
            {
                if (!s.startsWith(piece))
                {
                    result = false;
                    break loop;
                }
            }

            // If this is the last piece, then make sure the
            // string ends with it.
            if (i == len - 1)
            {
                if (s.endsWith(piece))
                {
                    result = true;
                }
                else
                {
                    result = false;
                }
                break loop;
            }

            // If this is neither the first or last piece, then
            // make sure the string contains it.
            if ((i > 0) && (i < (len - 1)))
            {
                index = s.indexOf(piece, index);
                if (index < 0)
                {
                    result = false;
                    break loop;
                }
            }

            // Move string index beyond the matching piece.
            index += piece.length();
        }

        return result;
    }

}
