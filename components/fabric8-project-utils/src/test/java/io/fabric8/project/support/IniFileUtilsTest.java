/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.project.support;

import org.junit.Test;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class IniFileUtilsTest {
    @Test
    public void testParseGitConfig() throws Exception {
        File basedir = new File(System.getProperty("basedir", "."));
        File file = new File(basedir, "src/test/resources/sample.gitconfig");
        assertThat(file).isFile().exists();

        Map<String, Properties> map = IniFileUtils.parseIniFile(file);
        System.out.println("Loaded: " + map);
        assertThat(map).containsKeys("user", "alias", "color");
        assertThat(map).hasSize(3);
        Properties user = map.get("user");
        assertThat(user).isNotNull().containsEntry("email", "fabric8@googlegroups.com");
    }

}
