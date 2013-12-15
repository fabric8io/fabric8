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
package io.fabric8.internal;

import junit.framework.Assert;
import org.junit.Test;

public class GeoLocationTest {
    @Test
    public void testGeoLocation(){
       String result = GeoUtils.getGeoLocation();
        Assert.assertNotNull(result);
        //could be an empty line if can't access geo location site
        if (!result.isEmpty()){
           Assert.assertFalse(result.contains(":"));
           int index = result.indexOf(',');
           Assert.assertTrue(index > 0);
        }
    }
}
