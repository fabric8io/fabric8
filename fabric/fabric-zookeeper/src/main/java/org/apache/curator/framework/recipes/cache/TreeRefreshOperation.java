/**
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
package org.apache.curator.framework.recipes.cache;

import com.google.common.base.Preconditions;

class TreeRefreshOperation implements Operation
{
    private final TreeCache cache;
    private final String path;
    private final TreeCache.RefreshMode mode;

    TreeRefreshOperation(TreeCache cache, String path, TreeCache.RefreshMode mode)
    {
        this.cache = Preconditions.checkNotNull(cache,"cache");
        this.path = Preconditions.checkNotNull(path,"path");
        this.mode = Preconditions.checkNotNull(mode,"mode");
    }

    @Override
    public void invoke() throws Exception
    {
        cache.refresh(path, mode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TreeRefreshOperation that = (TreeRefreshOperation) o;

        if (mode != that.mode) return false;
        if (path != null ? !path.equals(that.path) : that.path != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (mode != null ? mode.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "RefreshOperation(" + path + "," + mode + "){}";
    }
}
