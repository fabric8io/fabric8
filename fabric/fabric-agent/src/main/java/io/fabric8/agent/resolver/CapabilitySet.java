/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.fabric8.agent.resolver;

import org.osgi.framework.Constants;
import org.osgi.resource.Capability;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class CapabilitySet
{
    private final Map<String, Map<Object, Set<Capability>>> m_indices;
    private final Set<Capability> m_capSet = new HashSet<Capability>();

public void dump()
{
    for (Entry<String, Map<Object, Set<Capability>>> entry : m_indices.entrySet())
    {
        boolean header1 = false;
        for (Entry<Object, Set<Capability>> entry2 : entry.getValue().entrySet())
        {
            boolean header2 = false;
            for (Capability cap : entry2.getValue())
            {
                if (!header1)
                {
                    System.out.println(entry.getKey() + ":");
                    header1 = true;
                }
                if (!header2)
                {
                    System.out.println("   " + entry2.getKey());
                    header2 = true;
                }
                System.out.println("      " + cap);
            }
        }
    }
}

    public CapabilitySet(List<String> indexProps)
    {
        m_indices = new TreeMap<String, Map<Object, Set<Capability>>>();
        for (int i = 0; (indexProps != null) && (i < indexProps.size()); i++)
        {
            m_indices.put(
                indexProps.get(i), new HashMap<Object, Set<Capability>>());
        }
    }

    public void addCapability(Capability cap)
    {
        m_capSet.add(cap);

        // Index capability.
        for (Entry<String, Map<Object, Set<Capability>>> entry : m_indices.entrySet())
        {
            Object value = cap.getAttributes().get(entry.getKey());
            if (value != null)
            {
                if (value.getClass().isArray())
                {
                    value = convertArrayToList(value);
                }

                Map<Object, Set<Capability>> index = entry.getValue();

                if (value instanceof Collection)
                {
                    Collection c = (Collection) value;
                    for (Object o : c)
                    {
                        indexCapability(index, cap, o);
                    }
                }
                else
                {
                    indexCapability(index, cap, value);
                }
            }
        }
    }

    private void indexCapability(
        Map<Object, Set<Capability>> index, Capability cap, Object capValue)
    {
        Set<Capability> caps = index.get(capValue);
        if (caps == null)
        {
            caps = new HashSet<Capability>();
            index.put(capValue, caps);
        }
        caps.add(cap);
    }

    public void removeCapability(Capability cap)
    {
        if (m_capSet.remove(cap))
        {
            for (Entry<String, Map<Object, Set<Capability>>> entry : m_indices.entrySet())
            {
                Object value = cap.getAttributes().get(entry.getKey());
                if (value != null)
                {
                    if (value.getClass().isArray())
                    {
                        value = convertArrayToList(value);
                    }

                    Map<Object, Set<Capability>> index = entry.getValue();

                    if (value instanceof Collection)
                    {
                        Collection c = (Collection) value;
                        for (Object o : c)
                        {
                            deindexCapability(index, cap, o);
                        }
                    }
                    else
                    {
                        deindexCapability(index, cap, value);
                    }
                }
            }
        }
    }

    private void deindexCapability(
        Map<Object, Set<Capability>> index, Capability cap, Object value)
    {
        Set<Capability> caps = index.get(value);
        if (caps != null)
        {
            caps.remove(cap);
            if (caps.isEmpty())
            {
                index.remove(value);
            }
        }
    }

    public Set<Capability> match(SimpleFilter sf, boolean obeyMandatory)
    {
        Set<Capability> matches = match(m_capSet, sf);
        return (obeyMandatory)
            ? matchMandatory(matches, sf)
            : matches;
    }

    private Set<Capability> match(Set<Capability> caps, SimpleFilter sf)
    {
        Set<Capability> matches = new HashSet<Capability>();

        if (sf.getOperation() == SimpleFilter.MATCH_ALL)
        {
            matches.addAll(caps);
        }
        else if (sf.getOperation() == SimpleFilter.AND)
        {
            // Evaluate each subfilter against the remaining capabilities.
            // For AND we calculate the intersection of each subfilter.
            // We can short-circuit the AND operation if there are no
            // remaining capabilities.
            List<SimpleFilter> sfs = (List<SimpleFilter>) sf.getValue();
            for (int i = 0; (caps.size() > 0) && (i < sfs.size()); i++)
            {
                matches = match(caps, sfs.get(i));
                caps = matches;
            }
        }
        else if (sf.getOperation() == SimpleFilter.OR)
        {
            // Evaluate each subfilter against the remaining capabilities.
            // For OR we calculate the union of each subfilter.
            List<SimpleFilter> sfs = (List<SimpleFilter>) sf.getValue();
            for (int i = 0; i < sfs.size(); i++)
            {
                matches.addAll(match(caps, sfs.get(i)));
            }
        }
        else if (sf.getOperation() == SimpleFilter.NOT)
        {
            // Evaluate each subfilter against the remaining capabilities.
            // For OR we calculate the union of each subfilter.
            matches.addAll(caps);
            List<SimpleFilter> sfs = (List<SimpleFilter>) sf.getValue();
            for (int i = 0; i < sfs.size(); i++)
            {
                matches.removeAll(match(caps, sfs.get(i)));
            }
        }
        else
        {
            Map<Object, Set<Capability>> index = m_indices.get(sf.getName());
            if ((sf.getOperation() == SimpleFilter.EQ) && (index != null))
            {
                Set<Capability> existingCaps = index.get(sf.getValue());
                if (existingCaps != null)
                {
                    matches.addAll(existingCaps);
                    matches.retainAll(caps);
                }
            }
            else
            {
                for (Iterator<Capability> it = caps.iterator(); it.hasNext(); )
                {
                    Capability cap = it.next();
                    Object lhs = cap.getAttributes().get(sf.getName());
                    if (lhs != null)
                    {
                        if (compare(lhs, sf.getValue(), sf.getOperation()))
                        {
                            matches.add(cap);
                        }
                    }
                }
            }
        }

        return matches;
    }

    public static boolean matches(Capability cap, SimpleFilter sf)
    {
        return matchesInternal(cap, sf) && matchMandatory(cap, sf);
    }

    private static boolean matchesInternal(Capability cap, SimpleFilter sf)
    {
        boolean matched = true;

        if (sf.getOperation() == SimpleFilter.MATCH_ALL)
        {
            matched = true;
        }
        else if (sf.getOperation() == SimpleFilter.AND)
        {
            // Evaluate each subfilter against the remaining capabilities.
            // For AND we calculate the intersection of each subfilter.
            // We can short-circuit the AND operation if there are no
            // remaining capabilities.
            List<SimpleFilter> sfs = (List<SimpleFilter>) sf.getValue();
            for (int i = 0; matched && (i < sfs.size()); i++)
            {
                matched = matchesInternal(cap, sfs.get(i));
            }
        }
        else if (sf.getOperation() == SimpleFilter.OR)
        {
            // Evaluate each subfilter against the remaining capabilities.
            // For OR we calculate the union of each subfilter.
            matched = false;
            List<SimpleFilter> sfs = (List<SimpleFilter>) sf.getValue();
            for (int i = 0; !matched && (i < sfs.size()); i++)
            {
                matched = matchesInternal(cap, sfs.get(i));
            }
        }
        else if (sf.getOperation() == SimpleFilter.NOT)
        {
            // Evaluate each subfilter against the remaining capabilities.
            // For OR we calculate the union of each subfilter.
            List<SimpleFilter> sfs = (List<SimpleFilter>) sf.getValue();
            for (int i = 0; i < sfs.size(); i++)
            {
                matched = !(matchesInternal(cap, sfs.get(i)));
            }
        }
        else
        {
            matched = false;
            Object lhs = cap.getAttributes().get(sf.getName());
            if (lhs != null)
            {
                matched = compare(lhs, sf.getValue(), sf.getOperation());
            }
        }

        return matched;
    }

    private static Set<Capability> matchMandatory(
        Set<Capability> caps, SimpleFilter sf)
    {
        for (Iterator<Capability> it = caps.iterator(); it.hasNext(); )
        {
            Capability cap = it.next();
            if (!matchMandatory(cap, sf))
            {
                it.remove();
            }
        }
        return caps;
    }

    private static boolean matchMandatory(Capability cap, SimpleFilter sf)
    {
        if (cap instanceof CapabilityImpl) {
            for (Entry<String, Object> entry : cap.getAttributes().entrySet())
            {
                if (((CapabilityImpl) cap).isAttributeMandatory(entry.getKey())
                    && !matchMandatoryAttribute(entry.getKey(), sf))
                {
                    return false;
                }
            }
        } else {
            String value = cap.getDirectives().get(Constants.MANDATORY_DIRECTIVE);
            if (value != null) {
                List<String> names = ResourceBuilder.parseDelimitedString(value, ",");
                for (Entry<String, Object> entry : cap.getAttributes().entrySet())
                {
                    if (names.contains(entry.getKey())
                            && !matchMandatoryAttribute(entry.getKey(), sf))
                    {
                        return false;
                    }
                }
            }

        }
        return true;
    }

    private static boolean matchMandatoryAttribute(String attrName, SimpleFilter sf)
    {
        if ((sf.getName() != null) && sf.getName().equals(attrName))
        {
            return true;
        }
        else if (sf.getOperation() == SimpleFilter.AND)
        {
            List list = (List) sf.getValue();
            for (int i = 0; i < list.size(); i++)
            {
                SimpleFilter sf2 = (SimpleFilter) list.get(i);
                if ((sf2.getName() != null)
                    && sf2.getName().equals(attrName))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static final Class<?>[] STRING_CLASS = new Class[] { String.class };

    private static boolean compare(Object lhs, Object rhsUnknown, int op)
    {
        if (lhs == null)
        {
            return false;
        }

        // If this is a PRESENT operation, then just return true immediately
        // since we wouldn't be here if the attribute wasn't present.
        if (op == SimpleFilter.PRESENT)
        {
            return true;
        }

        // If the type is comparable, then we can just return the
        // result immediately.
        if (lhs instanceof Comparable)
        {
            // Spec says SUBSTRING is false for all types other than string.
            if ((op == SimpleFilter.SUBSTRING) && !(lhs instanceof String))
            {
                return false;
            }

            Object rhs;
            if (op == SimpleFilter.SUBSTRING)
            {
                rhs = rhsUnknown;
            }
            else
            {
                try
                {
                    rhs = coerceType(lhs, (String) rhsUnknown);
                }
                catch (Exception ex)
                {
                    return false;
                }
            }

            switch (op)
            {
                case SimpleFilter.EQ :
                    try
                    {
                        return (((Comparable) lhs).compareTo(rhs) == 0);
                    }
                    catch (Exception ex)
                    {
                        return false;
                    }
                case SimpleFilter.GTE :
                    try
                    {
                        return (((Comparable) lhs).compareTo(rhs) >= 0);
                    }
                    catch (Exception ex)
                    {
                        return false;
                    }
                case SimpleFilter.LTE :
                    try
                    {
                        return (((Comparable) lhs).compareTo(rhs) <= 0);
                    }
                    catch (Exception ex)
                    {
                        return false;
                    }
                case SimpleFilter.APPROX :
                    return compareApproximate(((Comparable) lhs), rhs);
                case SimpleFilter.SUBSTRING :
                    return SimpleFilter.compareSubstring((List<String>) rhs, (String) lhs);
                default:
                    throw new RuntimeException(
                        "Unknown comparison operator: " + op);
            }
        }
        // Booleans do not implement comparable, so special case them.
        else if (lhs instanceof Boolean)
        {
            Object rhs;
            try
            {
                rhs = coerceType(lhs, (String) rhsUnknown);
            }
            catch (Exception ex)
            {
                return false;
            }

            switch (op)
            {
                case SimpleFilter.EQ :
                case SimpleFilter.GTE :
                case SimpleFilter.LTE :
                case SimpleFilter.APPROX :
                    return (lhs.equals(rhs));
                default:
                    throw new RuntimeException(
                        "Unknown comparison operator: " + op);
            }
        }

        // If the LHS is not a comparable or boolean, check if it is an
        // array. If so, convert it to a list so we can treat it as a
        // collection.
        if (lhs.getClass().isArray())
        {
            lhs = convertArrayToList(lhs);
        }

        // If LHS is a collection, then call compare() on each element
        // of the collection until a match is found.
        if (lhs instanceof Collection)
        {
            for (Iterator iter = ((Collection) lhs).iterator(); iter.hasNext(); )
            {
                if (compare(iter.next(), rhsUnknown, op))
                {
                    return true;
                }
            }

            return false;
        }

        // Spec says SUBSTRING is false for all types other than string.
        if ((op == SimpleFilter.SUBSTRING) && !(lhs instanceof String))
        {
            return false;
        }

        // Since we cannot identify the LHS type, then we can only perform
        // equality comparison.
        try
        {
            return lhs.equals(coerceType(lhs, (String) rhsUnknown));
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    private static boolean compareApproximate(Object lhs, Object rhs)
    {
        if (rhs instanceof String)
        {
            return removeWhitespace((String) lhs)
                .equalsIgnoreCase(removeWhitespace((String) rhs));
        }
        else if (rhs instanceof Character)
        {
            return Character.toLowerCase(((Character) lhs))
                == Character.toLowerCase(((Character) rhs));
        }
        return lhs.equals(rhs);
    }

    private static String removeWhitespace(String s)
    {
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++)
        {
            if (!Character.isWhitespace(s.charAt(i)))
            {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private static Object coerceType(Object lhs, String rhsString) throws Exception
    {
        // If the LHS expects a string, then we can just return
        // the RHS since it is a string.
        if (lhs.getClass() == rhsString.getClass())
        {
            return rhsString;
        }

        // Try to convert the RHS type to the LHS type by using
        // the string constructor of the LHS class, if it has one.
        Object rhs = null;
        try
        {
            // The Character class is a special case, since its constructor
            // does not take a string, so handle it separately.
            if (lhs instanceof Character)
            {
                rhs = new Character(rhsString.charAt(0));
            }
            else
            {
                // Spec says we should trim number types.
                if ((lhs instanceof Number) || (lhs instanceof Boolean))
                {
                    rhsString = rhsString.trim();
                }
                Constructor ctor = lhs.getClass().getConstructor(STRING_CLASS);
                ctor.setAccessible(true);
                rhs = ctor.newInstance(new Object[] { rhsString });
            }
        }
        catch (Exception ex)
        {
            throw new Exception(
                "Could not instantiate class "
                    + lhs.getClass().getName()
                    + " from string constructor with argument '"
                    + rhsString + "' because " + ex);
        }

        return rhs;
    }

    /**
     * This is an ugly utility method to convert an array of primitives
     * to an array of primitive wrapper objects. This method simplifies
     * processing LDAP filters since the special case of primitive arrays
     * can be ignored.
     * @param array An array of primitive types.
     * @return An corresponding array using pritive wrapper objects.
    **/
    private static List convertArrayToList(Object array)
    {
        int len = Array.getLength(array);
        List list = new ArrayList(len);
        for (int i = 0; i < len; i++)
        {
            list.add(Array.get(array, i));
        }
        return list;
    }
}