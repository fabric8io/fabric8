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
package io.fabric8.cxf.endpoint;


import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import org.apache.cxf.common.logging.LogUtils;



public class JsonSchemaLookup {

    private static final Logger LOG = LogUtils.getL7dLogger(JsonSchemaLookup.class);

    private static JsonSchemaLookup singleton;

    private ObjectMapper mapper;

    public JsonSchemaLookup() {
    }

    public static JsonSchemaLookup getSingleton() {
        if (singleton == null) {
            // lazy create one
            new JsonSchemaLookup().init();
        }
        return singleton;
    }

    public void init() {
        LOG.log(Level.INFO, "Creating JsonSchemaLookup instance");
        try {
            if (mapper == null) {
                mapper = new ObjectMapper();

                mapper.setVisibility(new IgnorePropertiesBackedByTransientFields(mapper.getVisibilityChecker()));

                JaxbAnnotationModule module1 = new JaxbAnnotationModule();
                mapper.registerModule(module1);

                BeanValidationAnnotationModule module2 = new BeanValidationAnnotationModule();
                mapper.registerModule(module2);

            }
            // now lets expose the mbean...
            singleton = this;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Exception during initialization: ", e);
            throw new RuntimeException(e);
        }
    }

    

    
    

    public String getSchemaForClass(Class<?> clazz) {
        LOG.info("Looking up schema for " + clazz.getCanonicalName());
        String name = clazz.getName();
        try {
            ObjectWriter writer = mapper.writer().with(new FourSpacePrettyPrinter());
            JsonSchemaGenerator jsg = new JsonSchemaGenerator(mapper);
            JsonSchema jsonSchema = jsg.generateSchema(clazz);
            return writer.writeValueAsString(jsonSchema);
        } catch (Exception e) {
            LOG.log(Level.FINEST, "Failed to generate JSON schema for class " + name, e);
            return "";
        }
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }
}
