/**
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

package org.fusesource.eca.component.statistics;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.component.seda.SedaComponent;

/**
 * The statistics component.
 * <p/>
 * Valid properties are:
 * <ul>
 * <li>eventWindow - default =  "30s,1000" can be either depth based (numeric value), time based (ms,sec,min) or both</li>
 * <li>batchUpdateTime - default = null - a time based parameter for how often a stats message should be sent</li>
 * <li>cacheImplementation = "default" - the name of the cache implementation</li>
 * <li>queryString can be a comma separated list: If the first parameter equals a language, that will be used to create the query of the exchange,
 * else it will default to <I>simple<I></li>
 * <li>statisticsType  default = "ALL" - one of ALL, MEAN, MIN, MAX, VARIANCE, STDDEV, SKEWNESS, KUTOSIS, RATE - or a comma separated list of any of these</li>
 * </ul>
 */
public class StatisticsComponent extends SedaComponent {

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        int consumers = getAndRemoveParameter(parameters, "concurrentConsumers", Integer.class, 1);
        boolean limitConcurrentConsumers = getAndRemoveParameter(parameters, "limitConcurrentConsumers", Boolean.class, true);
        if (limitConcurrentConsumers && consumers > maxConcurrentConsumers) {
            throw new IllegalArgumentException("The limitConcurrentConsumers flag in set to true. ConcurrentConsumers cannot be set at a value greater than "
                    + maxConcurrentConsumers + " was " + consumers);
        }

        // defer creating queue till endpoint is started, so we pass in null
        StatisticsEndpoint answer = new StatisticsEndpoint(uri, this, null, consumers);
        answer.configureProperties(parameters);
        return answer;
    }

}
