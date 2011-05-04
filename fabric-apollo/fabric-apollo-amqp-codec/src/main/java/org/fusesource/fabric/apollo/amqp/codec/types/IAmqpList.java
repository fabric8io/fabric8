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
import java.util.List;

public interface IAmqpList<E extends AmqpType<?, ?>> extends Iterable<E> {

    public E get(int index);

    public void set(int index, E value);

    public int getListCount();

    public static class AmqpListIterator<E extends AmqpType<?, ?>> implements Iterator<E> {
        int next = 0;
        final IAmqpList<E> list;

        public AmqpListIterator(IAmqpList<E> list) {
            this.list = list;
        }

        public boolean hasNext() {
            return next < list.getListCount();
        }

        public E next() {
            return list.get(next++);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static abstract class AbstractAmqpList<E extends AmqpType<?, ?>> implements IAmqpList<E> {
        public static final int hashCodeFor(IAmqpList<?> l) {
            int hashCode = 1;
            for (Object obj : l) {
                hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
            }
            return hashCode;
        }

        public static final boolean checkEqual(IAmqpList<?> l1, IAmqpList<?> l2) {
            if (l1 == null ^ l2 == null) {
                return false;
            }

            if (l1 == null) {
                return true;
            }

            if (l1.getListCount() != l2.getListCount()) {
                return false;
            }

            Iterator<?> i1 = l1.iterator();
            Iterator<?> i2 = l2.iterator();
            while (i1.hasNext()) {
                Object e1 = i1.next();
                Object e2 = i2.next();
                if (!(e1 == null ? e2 == null : e1.equals(e2))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class AmqpWrapperList<E extends AmqpType<?, ?>> extends AbstractAmqpList<E> {
        private final List<E> list;

        public AmqpWrapperList(List<E> list) {
            this.list = list;
        }

        public E get(int index) {
            return list.get(index);
        }

        public int getListCount() {
            return list.size();
        }

        public void set(int index, E value) {
            list.set(index, value);
        }

        public Iterator<E> iterator() {
            return list.iterator();
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (o instanceof IAmqpList<?>) {
                return equals((IAmqpList<?>) o);
            }
            return false;
        }

        public boolean equals(IAmqpList<?> l) {
            return checkEqual(this, l);
        }

        public int hashCode() {
            return hashCodeFor(this);
        }
    }

    public static class ArrayBackedList<E extends AmqpType<?, ?>> extends AbstractAmqpList<E> {
        E[] list;

        public ArrayBackedList(E[] list) {
            this.list = list;
        }

        public E get(int index) {
            return list[index];
        }

        public int getListCount() {
            return list.length;
        }

        public void set(int index, E value) {
            list[index] = value;
        }

        public Iterator<E> iterator() {
            return new AmqpListIterator<E>(this);
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (o instanceof IAmqpList<?>) {
                return equals((IAmqpList<?>) o);
            }
            return false;
        }

        public boolean equals(IAmqpList<?> l) {
            return checkEqual(this, l);
        }

        public int hashCode() {
            return hashCodeFor(this);
        }
    }
}
