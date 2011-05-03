/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec.types;

import java.util.Iterator;
import java.util.Map;

public interface IAmqpMap<K extends AmqpType<?, ?>, V extends AmqpType<?, ?>> extends Iterable<Map.Entry<K, V>> {

    public void put(K key, V value);

    public V get(Object key);

    public int getEntryCount();

//    public void put(Object key, Object V);
//
//    public V get(Object o) throws AmqpEncodingError;


    public static abstract class AbstractAmqpMap<K extends AmqpType<?, ?>, V extends AmqpType<?, ?>> implements IAmqpMap<K, V> {

        public static final int hashCodeFor(IAmqpMap<?, ?> map) {
            int hashCode = 1;
            for (Map.Entry<?, ?> obj : map) {
                hashCode = 31 * hashCode + (obj.getKey() == null ? 0 : obj.getKey().hashCode());
                hashCode = 31 * hashCode + (obj.getValue() == null ? 0 : obj.getValue().hashCode());
            }
            return hashCode;
        }

        public static final <K extends AmqpType<?, ?>, V extends AmqpType<?,?>> boolean checkEqual(IAmqpMap<K, V> m1, IAmqpMap<?, ?> m2) {
            if (m1 == null ^ m2 == null) {
                return false;
            }

            if (m2 == null) {
                return true;
            }

            if (m1.getEntryCount() != m2.getEntryCount()) {
                return false;
            }

            for ( Object aM1 : m1 ) {
                Map.Entry<K, V> e1 = (Map.Entry<K, V>) aM1;
                Object v2 = m2.get(e1.getKey());
                if ( v2 == null ) {
                    return false;
                }
                if ( !(e1.getValue() == null ? v2 == null : e1.getValue().equals(v2)) ) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class AmqpWrapperMap<K extends AmqpType<?, ?>, V extends AmqpType<?, ?>> extends AbstractAmqpMap<K, V> {
        private Map<K, V> map;

        public AmqpWrapperMap(Map<K, V> map) {
            this.map = map;
        }

        public V get(Object key) {
            return map.get(key);
        }

        public Map.Entry<K, V> getEntry(final K key) {
            final V v = get(key);
            if (v == null) {
                return null;
            }
            return new Map.Entry<K, V>() {

                public K getKey() {
                    return key;
                }

                public V getValue() {
                    return v;
                }

                public V setValue(V value) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public int getEntryCount() {
            return map.size();
        }

        public void put(K key, V value) {
            map.put(key, value);
        }

        public Iterator<Map.Entry<K, V>> iterator() {
            return map.entrySet().iterator();
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (o instanceof IAmqpMap<?, ?>) {
                return checkEqual(this, (IAmqpMap<?, ?>) o);
            }
            return false;
        }

        public boolean equals(IAmqpMap<?, ?> m) {
            return checkEqual(this, m);
        }

        public int hashCode() {
            return hashCodeFor(this);
        }
    }
}
