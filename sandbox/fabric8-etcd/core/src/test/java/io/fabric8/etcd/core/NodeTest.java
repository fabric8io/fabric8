/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.etcd.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class NodeTest {

    @Test
    public void testBuilder() {
        ImmutableNode node = ImmutableNode.builder()
                .key("/key1")
                .value("value1")
                .createIndex(1)
                .modifiedIndex(2)
                .ttl(10)
                .expiration("expiration")
                .dir(false)
                .build();

        assertEquals("/key1", node.getKey());
        assertEquals("value1", node.getValue());
        assertEquals(1, node.getCreatedIndex());
        assertEquals(2, node.getModifiedIndex());
        assertEquals(10, node.getTtl());
        assertEquals("expiration", node.getExpiration());
        assertFalse(node.isDir());
    }

}