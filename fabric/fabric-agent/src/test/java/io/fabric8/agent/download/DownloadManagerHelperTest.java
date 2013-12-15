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
package io.fabric8.agent.download;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DownloadManagerHelperTest {

    @Test
    public void testStripUrl() {
        String artifact = "mvn:my/artifact/1.0";
        assertEquals(artifact, DownloadManagerHelper.stripUrl(artifact));
        assertEquals(artifact, DownloadManagerHelper.stripUrl("wrap:"+artifact));
        assertEquals(artifact, DownloadManagerHelper.stripUrl("wrap:"+artifact+"$Bundle-Version=1.1"));
        assertEquals(artifact, DownloadManagerHelper.stripUrl("war:"+artifact));
        assertEquals(artifact, DownloadManagerHelper.stripUrl("war:"+artifact+"?Webapp-Context=test"));
        assertEquals(artifact, DownloadManagerHelper.stripUrl("war:jar:"+artifact));
        assertEquals(artifact, DownloadManagerHelper.stripUrl("webbundle:"+artifact));
        assertEquals(artifact, DownloadManagerHelper.stripUrl("warref:"+artifact));
        assertEquals(artifact, DownloadManagerHelper.stripUrl("war-i:"+artifact));
        assertEquals(artifact, DownloadManagerHelper.stripUrl("spring:"+artifact));
        assertEquals(artifact, DownloadManagerHelper.stripUrl("blueprint:"+artifact));
    }
}
