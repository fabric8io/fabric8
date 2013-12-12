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
package io.fabric8.agent.mvn;

/**
 * Represents an artifact version segment. A segment is a version split into it's parts, parts separated by "." (dot)
 * or "-" (dash).
 * A segment can be of different forms such as integers or strings.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, January 30, 2008
 */
interface VersionSegment
        extends Comparable<VersionSegment> {

}
