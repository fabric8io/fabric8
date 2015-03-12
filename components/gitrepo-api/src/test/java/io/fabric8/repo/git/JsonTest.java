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
package io.fabric8.repo.git;

import org.junit.Test;

/**
 */
public class JsonTest {
    @Test
    public void testCreateRepository() throws Exception {
        CreateRepositoryDTO dto = new CreateRepositoryDTO();
        dto.setName("foo");
        dto.setDescription("some description");
        dto.setTeamId(1234);
        dto.setHasWiki(true);
        dto.setPrivateRepository(true);
        dto.setLicenseTemplate("something");

        String json = JsonHelper.toJson(dto);
        System.out.println("DTO " + dto + " is json: " + json);
    }

}
