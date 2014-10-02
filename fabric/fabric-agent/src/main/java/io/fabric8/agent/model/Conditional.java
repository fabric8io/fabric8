/*
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
package io.fabric8.agent.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditional", propOrder = {
        "condition",
        "config",
        "configfile",
        "feature",
        "bundle"
        })
public class Conditional extends Content {

    @XmlElement(name = "condition")
    protected List<String> condition;

    public List<String> getCondition() {
        if (condition == null) {
            this.condition = new ArrayList<>();
        }
        return condition;
    }

    public Feature asFeature(String name, String version) {
        String conditionName = name + "-condition-" + getConditionId().replaceAll("[^A-Za-z0-9 ]", "_");
        Feature f = new Feature(conditionName, version);
        f.getBundle().addAll(getBundle());
        f.getConfig().addAll(getConfig());
        f.getConfigfile().addAll(getConfigfile());
        f.getFeature().addAll(getFeature());
        return f;
    }

    private String getConditionId() {
        StringBuffer sb = new StringBuffer();
        for (String cond : getCondition()) {
            if (sb.length() > 0) {
                sb.append("_");
            }
            sb.append(cond);
        }
        return sb.toString();
    }
}
