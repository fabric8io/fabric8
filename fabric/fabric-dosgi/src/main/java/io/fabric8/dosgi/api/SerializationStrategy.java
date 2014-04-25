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
package io.fabric8.dosgi.api;

import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

/**
 * <p>
 * </p>
 *
 */
public interface SerializationStrategy {

    String name();

    void encodeRequest(ClassLoader loader, Class<?>[] types, Object[] args, DataByteArrayOutputStream target) throws Exception;

    void decodeResponse(ClassLoader loader, Class<?> type, DataByteArrayInputStream source, AsyncCallback result) throws Exception;

    void decodeRequest(ClassLoader loader, Class<?>[] types, DataByteArrayInputStream source, Object[] target) throws Exception;

    void encodeResponse(ClassLoader loader, Class<?> type, Object value, Throwable error, DataByteArrayOutputStream target) throws Exception;

}
