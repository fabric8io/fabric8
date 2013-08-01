/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.dozer.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.converter.dozer.DozerTypeConverterLoader;
import org.dozer.DozerBeanMapper;

public class WatcherDozerTypeConverterLoaded extends DozerTypeConverterLoader {

    // TODO: Needs a new build to work with camel-dozer

    private String mappingFile;

    public WatcherDozerTypeConverterLoaded() {
    }

    public String getMappingFile() {
        return mappingFile;
    }

    public void setMappingFile(String mappingFile) {
        this.mappingFile = mappingFile;
    }

    @Override
    protected Map<String, DozerBeanMapper> lookupDozerBeanMappers() {
        List<String> mappingFiles = new ArrayList<String>(1);
        mappingFiles.add(mappingFile);

        DozerBeanMapper mapper = new DozerBeanMapper(mappingFiles);
        Map<String, DozerBeanMapper> answer = new HashMap<String, DozerBeanMapper>(1);
        answer.put("dozer", mapper);
        return answer;
    }
}
