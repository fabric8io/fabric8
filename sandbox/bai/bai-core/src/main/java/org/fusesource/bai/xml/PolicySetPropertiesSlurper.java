/*
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

package org.fusesource.bai.xml;

import org.fusesource.bai.config.EventType;
import org.fusesource.bai.config.Policy;
import org.fusesource.bai.config.PolicySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This implementation of {@link PolicySlurper} reads a OSGi Config Admin-compatible text property file and constructs the PolicySet.
 *
 * @author Raul Kripalani
 */
@SuppressWarnings("rawtypes")
public class PolicySetPropertiesSlurper implements PolicySlurper {

    private static final transient Logger LOG = LoggerFactory.getLogger(PolicySetPropertiesSlurper.class);

    private Dictionary properties;
    private PolicySet policyCache;

    public PolicySetPropertiesSlurper(Dictionary properties) {
        this.properties = properties;
    }

    public Dictionary getProperties() {
        return properties;
    }

    public void setProperties(Dictionary properties) {
        this.properties = properties;
    }


    public PolicySet refresh() {
        policyCache = slurp();
        return policyCache;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized PolicySet slurp() {
        PolicySet answer = new PolicySet();
        List<Object> keys = Collections.list(properties.keys());

        for (Object k : keys) {
            String key = (String) k;
            String value = (String) properties.get(key);
            if (key.startsWith("bai.")) {
                String[] splitKey = key.substring(4).split("\\.", 3);

                if (splitKey != null) {
                    if (splitKey.length >= 2) {
                        String id = splitKey[0];
                        String qualifier = splitKey[1];
                        Policy policy = answer.policy(id);
                        if ("enabled".equals(qualifier)) {
                            Boolean flag = parseBoolean(key, value);
                            if (flag != null) {
                                policy.setEnabled(flag);
                            }
                        } else if ("to".equals(qualifier)) {
                            policy.setTo(value);
                        } else if (splitKey.length == 3) {
                            if ("filter".equals(qualifier)) {
                                policy.filter().language(splitKey[2], value);
                            } else {
                                boolean include = isInclude(splitKey[2]);
                                List<String> patterns = splitPatterns(value);
                                if ("context".equals(qualifier)) {
                                    for (String pattern : patterns) {
                                        policy.contexts().addPattern(include, value);
                                    }
                                } else if ("endpoint".equals(qualifier)) {
                                    for (String pattern : patterns) {
                                        policy.endpoints().addPattern(include, pattern);
                                    }
                                } else if ("event".equals(qualifier)) {
                                    for (String pattern : patterns) {
                                        EventType eventType = EventType.parseEventType(pattern);
                                        if (eventType != null) {
                                            policy.events().addEvent(include, eventType);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        policyCache = answer;
        return answer;
    }

    private Boolean parseBoolean(String key, String value) {
        try {
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            LOG.error("Failed to parse boolean '" + value + "' for key " + key + ". Reason: " + e, e);
            return null;
        }
    }

    public PolicySet getPolicies() {
        return policyCache;
    }

    private List<String> splitPatterns(String value) {
        List<String> answer = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(value);
        while (tokenizer.hasMoreTokens()) {
            answer.add(tokenizer.nextToken());
        }
        return answer;
    }

    protected boolean isInclude(String includeOrExclude) {
        return includeOrExclude.equalsIgnoreCase("include");
    }

}
