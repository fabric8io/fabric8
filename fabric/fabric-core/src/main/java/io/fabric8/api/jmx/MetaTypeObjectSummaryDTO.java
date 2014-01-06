/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api.jmx;

import org.fusesource.insight.log.support.Strings;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class MetaTypeObjectSummaryDTO extends MetaTypeObjectSupportDTO {
    private List<Long> pidBundleIds = new ArrayList<Long>();
    private List<Long> factoryPidBundleIds = new ArrayList<Long>();

    public MetaTypeObjectSummaryDTO() {
    }

    public MetaTypeObjectSummaryDTO(String id) {
        super(id);
    }

    public List<Long> getPidBundleIds() {
        return pidBundleIds;
    }

    public void setPidBundleIds(List<Long> pidBundleIds) {
        this.pidBundleIds = pidBundleIds;
    }

    public List<Long> getFactoryPidBundleIds() {
        return factoryPidBundleIds;
    }

    public void setFactoryPidBundleIds(List<Long> factoryPidBundleIds) {
        this.factoryPidBundleIds = factoryPidBundleIds;
    }

}
