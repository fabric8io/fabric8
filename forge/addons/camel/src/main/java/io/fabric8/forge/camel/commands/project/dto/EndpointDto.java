/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.camel.commands.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.forge.camel.commands.project.model.CamelEndpointDetails;

/**
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EndpointDto {
    private String fileName;
    private String lineNumber;
    private String endpointComponentName;
    private String endpointInstance;
    private String endpointUri;
    private boolean consumerOnly;
    private boolean producerOnly;

    public EndpointDto() {
    }

    public EndpointDto(CamelEndpointDetails details) {
        this.fileName = details.getFileName();
        this.lineNumber = details.getLineNumber();
        this.endpointComponentName = details.getEndpointComponentName();
        this.endpointInstance = details.getEndpointInstance();
        this.endpointUri = details.getEndpointUri();
        this.consumerOnly = details.isConsumerOnly();
        this.producerOnly = details.isProducerOnly();
    }

    public boolean isConsumerOnly() {
        return consumerOnly;
    }

    public void setConsumerOnly(boolean consumerOnly) {
        this.consumerOnly = consumerOnly;
    }

    public String getEndpointComponentName() {
        return endpointComponentName;
    }

    public void setEndpointComponentName(String endpointComponentName) {
        this.endpointComponentName = endpointComponentName;
    }

    public String getEndpointInstance() {
        return endpointInstance;
    }

    public void setEndpointInstance(String endpointInstance) {
        this.endpointInstance = endpointInstance;
    }

    public String getEndpointUri() {
        return endpointUri;
    }

    public void setEndpointUri(String endpointUri) {
        this.endpointUri = endpointUri;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isProducerOnly() {
        return producerOnly;
    }

    public void setProducerOnly(boolean producerOnly) {
        this.producerOnly = producerOnly;
    }
}
