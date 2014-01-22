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

package io.fabric8.insight.elasticsearch;

import java.io.IOException;

/**
 * Interface to send REST based request to the ES cluster
 */
public interface ElasticRest {

    String get(String uri) throws IOException;

    String post(String uri, String content) throws IOException;

    String put(String uri, String content) throws IOException;

    String delete(String uri) throws IOException;

    String head(String uri) throws IOException;

}
