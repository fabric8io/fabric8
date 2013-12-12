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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CapabilitySet<C extends Capability>
{
    private final Map<String, Map<Object, Set<C>>> m_indices;
    private final Set<C> m_capSet = new HashSet<C>();
    private final ReadWriteLock m_lock = new ReentrantReadWriteLock();

    public CapabilitySet(List<String> indexProps, boolean caseSensitive)
    {
        m_indices = (caseSensitive)
            ? new TreeMap<String, Map<Object, Set<C>>>()
            : new TreeMap<String, Map<Object, Set<C>>>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; (indexProps != null) && (i < indexProps.size()); i++)
        {
            m_indices.put(indexProps.get(i), new HashMap<Object, Set<C>>());
        }
    }

    public void addCapability(C cap)
    {
        m_lock.writeLock().lock();
        try
        {
            doAddCapability(cap);
        }
        finally
        {
            m_lock.writeLock().unlock();
        }
    }

    private void doAddCapability(C cap)
    {
        m_capSet.add(cap);

        // Index capability.
        for (Entry<String, Map<Object, Set<C>>> entry : m_indices.entrySet())
        {
            Attribute capAttr = cap.getAttribute(entry.getKey());
            if (capAttr != null)
            {
                Object capValue = capAttr.getValue();
                if (capValue.getClass().isArray())
                {
                    capValue = convertArrayToList(capValue);
                }

                Map<Object, Set<C>> index = entry.getValue();

                if (capValue instanceof Collection)
                {
                    Collection c = (Collection) capValue;
                    for (Object o : c)
                    {
                        indexCapability(index, cap, o);
                    }
                }
                else
                {
                    indexCapability(index, cap, capValue);
                }
            }
        }
    }

    private void indexCapability(
        Map<Object, Set<C>> index, C cap, Object capValue)
    {
        Set<C> caps = index.get(capValue);
        if (caps == null)
        {
            caps = new HashSet<C>();
            index.put(capValue, caps);
        }
        caps.add(cap);
    }

    public void removeCapability(C cap)
    {
        m_lock.writeLock().lock();
        try
        {
            doRemoveCapability(cap);
        }
        finally
        {
            m_lock.writeLock().unlock();
        }
    }

    private void doRemoveCapability(C cap)
    {
        if (m_capSet.remove(cap))
        {
            for (Entry<String, Map<Object, Set<C>>> entry : m_indices.entrySet())
            {
                Attribute capAttr = cap.getAttribute(entry.getKey());
                if (capAttr != null)
                {
                    Object capValue = capAttr.getValue();
                    if (capValue.getClass().isArray())
                    {
                        capValue = convertArrayToList(capValue);
                    }

                    Map<Object, Set<C>> index = entry.getValue();

                    if (capValue instanceof Collection)
                    {
                        Collection c = (Collection) capValue;
                        for (Object o : c)
                        {
                            deindexCapability(index, cap, o);
                        }
                    }
                    else
                    {
                        deindexCapability(index, cap, capValue);
                    }
                }
            }
        }
    }

    private void deindexCapability(
        Map<Object, Set<C>> index, C cap, Object capValue)
    {
        Set<C> caps = index.get(capValue);
        if (caps != null)
        {
            caps.remove(cap);
            if (caps.isEmpty())
            {
                index.remove(capValue);
            }
        }
    }

    public Set<C> match(SimpleFilter sf)
    {
        m_lock.readLock().lock();
        try
        {
            return match(m_capSet, sf);
        }
        finally
        {
            m_lock.readLock().unlock();
        }
    }

    private Set<C> match(Set<C> caps, SimpleFilter sf)
    {
        Set<C> matches = new HashSet<C>();

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
            Map<Object, Set<C>> index = m_indices.get(sf.getName());
            if ((sf.getOperation() == SimpleFilter.EQ) && (index != null))
            {
                Set<C> existingCaps = index.get(sf.getValue());
                if (existingCaps != null)
                {
                    matches.addAll(existingCaps);
                    matches.retainAll(caps);
                }
            }
            else
            {
                for (Iterator<C> it = caps.iterator(); it.hasNext(); )
                {
                    C cap = it.next();
                    Attribute attr = cap.getAttribute(sf.getName());
                    if (attr != null)
                    {
                        Object lhs = attr.getValue();
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
        return matchesInternal(cap, sf);
    }

    private static boolean matchesInternal(Capability cap, SimpleFilter sf)
    {
        boolean matched = true;

        if (sf.getOperation() == SimpleFilter.AND)
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
            Attribute attr = cap.getAttribute(sf.getName());
            if (attr != null)
            {
                Object lhs = attr.getValue();
                matched = compare(lhs, sf.getValue(), sf.getOperation());
            }
        }

        return matched;
    }

    private static final Class[] STRING_CLASS = new Class[] { String.class };

    private static boolean compare(Object lhs, Object rhsUnknown, int op)
    {
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
                    return (((Comparable) lhs).compareTo(rhs) == 0);
                case SimpleFilter.GTE :
                    return (((Comparable) lhs).compareTo(rhs) >= 0);
                case SimpleFilter.LTE :
                    return (((Comparable) lhs).compareTo(rhs) <= 0);
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
// TODO: COMPLIANCE - This should be changed to return false in case
//       of an exception, but the R4.2 CT has a mistake in it, so for
//       now we'll throw exceptions from equals().
//        try
//        {
//            return lhs.equals(coerceType(lhs, (String) rhsUnknown));
//        }
//        catch (Exception ex)
//        {
//            return false;
//        }
        Object rhsObj = null;
        try
        {
            rhsObj = coerceType(lhs, (String) rhsUnknown);
        }
        catch (Exception ex)
        {
            return false;
        }
        return lhs.equals(rhsObj);
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
        Object rhs;
        try
        {
            // The Character class is a special case, since its constructor
            // does not take a string, so handle it separately.
            if (lhs instanceof Character)
            {
                rhs = rhsString.charAt(0);
            }
            else
            {
                Constructor ctor = lhs.getClass().getConstructor(STRING_CLASS);
                ctor.setAccessible(true);
                rhs = ctor.newInstance(rhsString);
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