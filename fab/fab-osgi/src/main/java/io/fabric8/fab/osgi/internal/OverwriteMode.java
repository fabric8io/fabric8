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

package io.fabric8.fab.osgi.internal;

/**
 * Strategy to use regarding manifest rewrite, for a jar that is already a bundle (has osgi manifest attributes).
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 1.1.1, September 17, 2009
 */
public enum OverwriteMode
{

    /**
     * Keep existing manifest.
     */
    KEEP,

    /**
     * Merge instructions with current manifest entries.
     */
    MERGE,

    /**
     * Full rewrite.
     */
    FULL

}
