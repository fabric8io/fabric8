/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.cluster.util;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.hawtbuf.codec.Codec;
import org.fusesource.hawtbuf.codec.VariableCodec;

import java.io.IOException;
import java.io.DataOutput;
import java.io.DataInput;
import java.util.zip.Checksum;
import java.util.zip.CRC32;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Implementing this interface allows an object to customize
 * how hash values are computed for Node objects and Key objects.
 *
 * @param <N>
 * @param <K>
 */
public interface Hasher<N, K> {

    int hashNode(N node, int i);

    int hashKey(K value);

    /**
     * This Hasher implementation works with any type of object by using
     * the Object.hashCode() method of the Node and Keys objects
     * to compute the hash.  The node object MUST implement a toString()
     * method which returns a unique id for the Node.
     */
    public class Native implements Hasher {
        /**
         * @param node
         * @param i
         * @return (node.toString()+":"+i).hashCode();
         */
        public int hashNode(Object node, int i) {
            return (node.toString() + ":" + i).hashCode();
        }

        /**
         * @param value
         * @return value.hashCode();
         */
        public int hashKey(Object value) {
            return value.hashCode();
        }
    }


    /**
     * A Hasher implemenation which first convert the Node and Key to
     * byte arrays before calculating the hash using a HashAlgorithim.
     *
     * @param <N>
     * @param <K>
     */
    public class BinaryHasher<N, K> implements Hasher<N, K> {
        private final Codec<N> nodeCodec;
        private final Codec<K> keyCodec;
        private final HashAlgorithim hashAlgorithim;

        public BinaryHasher(Codec<N> nodeCodec, Codec<K> keyCodec, HashAlgorithim hashAlgorithim) {
            this.nodeCodec = nodeCodec;
            this.keyCodec = keyCodec;
            this.hashAlgorithim = hashAlgorithim;
        }

        public int hashNode(N node, int i) {
            try {
                DataByteArrayOutputStream os = new DataByteArrayOutputStream();
                nodeCodec.encode(node, os);
                os.write(':');
                os.writeInt(i);
                return hash(os.toBuffer());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public int hashKey(K value) {
            try {
                DataByteArrayOutputStream os = new DataByteArrayOutputStream();
                keyCodec.encode(value, os);
                return hash(os.toBuffer());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public int hash(Buffer buffer) {
            return hashAlgorithim.hash(buffer.data, buffer.length);
        }
    }

    /**
     * Used to calculate the hash of a binary buffer.
     */
    public interface HashAlgorithim {
        int hash(byte[] data, int len);
    }

    /**
     * A HashAlgorithim instance which use a MessageDigest
     * algorithim to compute the hash value.
     */
    public class MessageDigestFactory implements HashAlgorithim {
        String algorithim;

        public MessageDigestFactory(String algorithim) {
            this.algorithim = algorithim;
        }

        public int hash(byte[] data, int len) {
            try {
                MessageDigest md = MessageDigest.getInstance(algorithim);
                md.update(data, 0, len);
                byte[] digest = md.digest();
                // Return the high bytes bytes of the digest as an int
                return (int)
                        ((digest[0] & 0xFF) << 24)
                        | ((digest[1] & 0xFF) << 16)
                        | ((digest[2] & 0xFF) << 8)
                        | (digest[3] & 0xFF);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(algorithim + " not supported", e);
            }
        }
    }

    public static HashAlgorithim MD5 = new MessageDigestFactory("MD5");

    long INTEGER_MASK = 0xFFFFFFFFL;

    /**
     * A HashAlgorithim instance which uses the Fowler/Noll/Vo FNV-1a hash
     * algorithim to compute the hash value.
     * <p/>
     * see: http://en.wikipedia.org/wiki/Fowler-Noll-Vo_hash_function
     */
    public static HashAlgorithim FNV1A = new HashAlgorithim() {
        private static final long INIT = 0xcbf29ce484222325L;
        private static final long PRIME = 1099511628211L;

        public int hash(byte[] b, int len) {
            long value = INIT;
            for (int i = 0; i < len; i++) {
                value ^= b[i];
                value *= PRIME;
            }
            return (int)((value >> 16) & INTEGER_MASK);
        }
    };

    /**
     * A HashAlgorithim instance which uses the Murmur hash algorithim
     * to compute the hash value.
     * <p/>
     * lifted from: https://svn.apache.org/repos/asf/hadoop/hbase/trunk/src/java/org/apache/hadoop/hbase/util/MurmurHash.java
     * <p/>
     *
     * This is a very fast, non-cryptographic hash suitable for general hash-based
     * lookup.  See http://murmurhash.googlepages.com/ for more details.
     *
     * <p>The C version of MurmurHash 2.0 found at that site was ported
     * to Java by Andrzej Bialecki (ab at getopt org).</p>
     */
    public static HashAlgorithim MURMUR = new HashAlgorithim() {
        private static final int seed = 0xcbf29ce4;
        public int hash(byte[] data, int length) {
            int m = 0x5bd1e995;
            int r = 24;

            int h = seed ^ length;

            int len_4 = length >> 2;

            for (int i = 0; i < len_4; i++) {
                int i_4 = i << 2;
                int k = data[i_4 + 3];
                k = k << 8;
                k = k | (data[i_4 + 2] & 0xff);
                k = k << 8;
                k = k | (data[i_4 + 1] & 0xff);
                k = k << 8;
                k = k | (data[i_4 + 0] & 0xff);
                k *= m;
                k ^= k >>> r;
                k *= m;
                h *= m;
                h ^= k;
            }

            // avoid calculating modulo
            int len_m = len_4 << 2;
            int left = length - len_m;

            if (left != 0) {
                if (left >= 3) {
                    h ^= data[length - 3] << 16;
                }
                if (left >= 2) {
                    h ^= data[length - 2] << 8;
                }
                if (left >= 1) {
                    h ^= data[length - 1];
                }

                h *= m;
            }

            h ^= h >>> 13;
            h *= m;
            h ^= h >>> 15;

            return h;
        }
    };


    /**
     * A HashAlgorithim instance which uses the Jenkins hash algorithim
     * to compute the hash value.
     * <p/>
     * lifted from: https://svn.apache.org/repos/asf/hadoop/hbase/trunk/src/java/org/apache/hadoop/hbase/util/MurmurHash.java
     * <p/>
     *
     * <pre>lookup3.c, by Bob Jenkins, May 2006, Public Domain.
     *
     * You can use this free for any purpose.  It's in the public domain.
     * It has no warranty.
     * </pre>
     *
     * @see <a href="http://burtleburtle.net/bob/c/lookup3.c">lookup3.c</a>
     * @see <a href="http://www.ddj.com/184410284">Hash Functions (and how this
     * function compares to others such as CRC, MD?, etc</a>
     * @see <a href="http://burtleburtle.net/bob/hash/doobs.html">Has update on the
     * Dr. Dobbs Article</a>
     */
    public static HashAlgorithim JENKINS = new HashAlgorithim() {
        private static final long INIT = 0xcbf29ce484222325L;
        private final long BYTE_MASK = 0x00000000000000ffL;

        private long rot(long val, int pos) {
          return ((Integer.rotateLeft( (int)(val & INTEGER_MASK), pos)) & INTEGER_MASK);
        }

        public int hash(byte[] key, int nbytes) {
          int length = nbytes;
          long a, b, c;       // We use longs because we don't have unsigned ints
          a = b = c = (0x00000000deadbeefL + length + INIT) & INTEGER_MASK;
          int offset = 0;
          for (; length > 12; offset += 12, length -= 12) {

            a = (a + (key[offset + 0]    & BYTE_MASK)) & INTEGER_MASK;
            a = (a + (((key[offset + 1]  & BYTE_MASK) <<  8) & INTEGER_MASK)) & INTEGER_MASK;
            a = (a + (((key[offset + 2]  & BYTE_MASK) << 16) & INTEGER_MASK)) & INTEGER_MASK;
            a = (a + (((key[offset + 3]  & BYTE_MASK) << 24) & INTEGER_MASK)) & INTEGER_MASK;
            b = (b + (key[offset + 4]    & BYTE_MASK)) & INTEGER_MASK;
            b = (b + (((key[offset + 5]  & BYTE_MASK) <<  8) & INTEGER_MASK)) & INTEGER_MASK;
            b = (b + (((key[offset + 6]  & BYTE_MASK) << 16) & INTEGER_MASK)) & INTEGER_MASK;
            b = (b + (((key[offset + 7]  & BYTE_MASK) << 24) & INTEGER_MASK)) & INTEGER_MASK;
            c = (c + (key[offset + 8]    & BYTE_MASK)) & INTEGER_MASK;
            c = (c + (((key[offset + 9]  & BYTE_MASK) <<  8) & INTEGER_MASK)) & INTEGER_MASK;
            c = (c + (((key[offset + 10] & BYTE_MASK) << 16) & INTEGER_MASK)) & INTEGER_MASK;
            c = (c + (((key[offset + 11] & BYTE_MASK) << 24) & INTEGER_MASK)) & INTEGER_MASK;

            a = (a - c) & INTEGER_MASK;  a ^= rot(c, 4);  c = (c + b) & INTEGER_MASK;
            b = (b - a) & INTEGER_MASK;  b ^= rot(a, 6);  a = (a + c) & INTEGER_MASK;
            c = (c - b) & INTEGER_MASK;  c ^= rot(b, 8);  b = (b + a) & INTEGER_MASK;
            a = (a - c) & INTEGER_MASK;  a ^= rot(c,16);  c = (c + b) & INTEGER_MASK;
            b = (b - a) & INTEGER_MASK;  b ^= rot(a,19);  a = (a + c) & INTEGER_MASK;
            c = (c - b) & INTEGER_MASK;  c ^= rot(b, 4);  b = (b + a) & INTEGER_MASK;
          }

          //-------------------------------- last block: affect all 32 bits of (c)
          switch (length) {                   // all the case statements fall through
          case 12:
            c = (c + (((key[offset + 11] & BYTE_MASK) << 24) & INTEGER_MASK)) & INTEGER_MASK;
          case 11:
            c = (c + (((key[offset + 10] & BYTE_MASK) << 16) & INTEGER_MASK)) & INTEGER_MASK;
          case 10:
            c = (c + (((key[offset + 9]  & BYTE_MASK) <<  8) & INTEGER_MASK)) & INTEGER_MASK;
          case  9:
            c = (c + (key[offset + 8]    & BYTE_MASK)) & INTEGER_MASK;
          case  8:
            b = (b + (((key[offset + 7]  & BYTE_MASK) << 24) & INTEGER_MASK)) & INTEGER_MASK;
          case  7:
            b = (b + (((key[offset + 6]  & BYTE_MASK) << 16) & INTEGER_MASK)) & INTEGER_MASK;
          case  6:
            b = (b + (((key[offset + 5]  & BYTE_MASK) <<  8) & INTEGER_MASK)) & INTEGER_MASK;
          case  5:
            b = (b + (key[offset + 4]    & BYTE_MASK)) & INTEGER_MASK;
          case  4:
            a = (a + (((key[offset + 3]  & BYTE_MASK) << 24) & INTEGER_MASK)) & INTEGER_MASK;
          case  3:
            a = (a + (((key[offset + 2]  & BYTE_MASK) << 16) & INTEGER_MASK)) & INTEGER_MASK;
          case  2:
            a = (a + (((key[offset + 1]  & BYTE_MASK) <<  8) & INTEGER_MASK)) & INTEGER_MASK;
          case  1:
            a = (a + (key[offset + 0]    & BYTE_MASK)) & INTEGER_MASK;
            break;
          case  0:
            return (int)(c & INTEGER_MASK);
          }
          c ^= b; c = (c - rot(b,14)) & INTEGER_MASK;
          a ^= c; a = (a - rot(c,11)) & INTEGER_MASK;
          b ^= a; b = (b - rot(a,25)) & INTEGER_MASK;
          c ^= b; c = (c - rot(b,16)) & INTEGER_MASK;
          a ^= c; a = (a - rot(c,4))  & INTEGER_MASK;
          b ^= a; b = (b - rot(a,14)) & INTEGER_MASK;
          c ^= b; c = (c - rot(b,24)) & INTEGER_MASK;

          return (int)(c & INTEGER_MASK);
        }
    };

    /**
     * A HashAlgorithim instance which uses CRC32 checksum
     * algorithim to compute the hash value.
     */
    public static HashAlgorithim CRC32 = new HashAlgorithim() {
        public int hash(byte[] data, int len) {
            Checksum checksum =  new CRC32();
            checksum.update(data, 0, len);
            return (int) (((checksum.getValue() >> 32) ^ checksum.getValue()) & INTEGER_MASK);
        }
    };

    /**
     * Used to convert an object to a byte[] by basically doing:
     * Object.toString().getBytes("UTF-8")
     */
    public class ToStringCodec extends VariableCodec<Object> {
        public void encode(Object o, DataOutput dataOutput) throws IOException {
            dataOutput.write(o.toString().getBytes("UTF-8"));
        }

        public Object decode(DataInput dataInput) throws IOException {
            throw new UnsupportedOperationException();
        }

        public int estimatedSize(Object object) {
            return object.toString().length();
        }
    }

    /**
     * This Hasher implementation works with any type of object by using
     * Object.toString() and uses a checksum to compute the hash of
     * the key and value.
     */
    public class ToStringHasher extends BinaryHasher<Object, Object> {

        /**
         * Constructs a ToStringChecksumHasher that uses the JENKINS hash algorithim. 
         */
        public ToStringHasher() {
            this(JENKINS);
        }

        public ToStringHasher(HashAlgorithim hashAlgorithim) {
            super(new ToStringCodec(), new ToStringCodec(), hashAlgorithim);
        }

        @Override
        public int hashNode(Object node, int i) {
            return super.hashKey(node.toString() + ":" + i);
        }
    }

    ;


}
