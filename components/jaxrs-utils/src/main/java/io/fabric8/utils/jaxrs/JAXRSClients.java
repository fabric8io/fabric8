/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.utils.jaxrs;

import javax.ws.rs.WebApplicationException;
import java.util.concurrent.Callable;

/**
 */
public class JAXRSClients {
    /**
     * A helper method to handle REST APIs which throw a 404 by just returning null
     */
    public static <T> T handle404ByReturningNull(Callable<T> callable) {
        try {
            return callable.call();
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                return null;
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }
}
