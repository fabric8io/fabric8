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
package io.fabric8.groups.internal;

import io.fabric8.groups.NodeState;

class UpdateOperation<T extends NodeState> implements Operation
{
    private final ZooKeeperGroup<T> cache;
    private final T node;

    UpdateOperation(ZooKeeperGroup cache, T node)
    {
        this.cache = cache;
        this.node = node;
    }

    @Override
    public void invoke() throws Exception
    {
        cache.doUpdate(node);
    }

    @Override
    public boolean equals(Object o)
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        return getClass().hashCode();
    }

    @Override
    public String toString()
    {
        return "UpdateOperation{" +
                "node='" + node + '\'' +
                '}';
    }
}
