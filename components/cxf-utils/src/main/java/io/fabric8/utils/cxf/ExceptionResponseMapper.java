/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.utils.cxf;

import io.fabric8.utils.IOHelpers;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.InputStream;

public class ExceptionResponseMapper implements ResponseExceptionMapper<Exception> {

    @Override
    public Exception fromResponse(Response response) {
        try {
            Object entity = response.getEntity();
            String message = "No message";
            if (entity instanceof InputStream) {
                InputStream inputStream = (InputStream) entity;
                message = IOHelpers.readFully(inputStream);
            } else if (entity != null) {
                message = entity.toString();
            }
            message = "HTTP " + response.getStatus() + " " + message;
            return new WebApplicationException(message, response);
        } catch (Exception ex) {
            return new Exception("Could not deserialize server side exception: " + ex.getMessage());
        }
    }

}
