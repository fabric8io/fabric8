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
package io.fabric8.api.jmx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;





public class HrefResource {
    public String name;
    public List<HashMap<String, String>> links= new ArrayList<HashMap<String,String>>();
    public Map<String, String> attribute= new HashMap<String,String>();
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public HrefResource(final String name, final String rel, final String href, String method){
        setName(name);
        links.add(new HashMap<String, String>() {
            {
                put("rel", rel);
                put("href", href);
            }});
        attribute.put("method",method);
    }
    public Map<String, String> getAttribute() {
        return attribute;
    }
    public void setAttribute(Map<String, String> attribute) {
        this.attribute = attribute;
    }
}