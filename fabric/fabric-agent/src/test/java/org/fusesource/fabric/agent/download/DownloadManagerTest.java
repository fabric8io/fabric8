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
package org.fusesource.fabric.agent.download;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DownloadManagerTest {

    @Test
    public void testStripUrl() {
        String artifact = "mvn:my/artifact/1.0";
        assertEquals(artifact, DownloadManager.stripUrl(artifact));
        assertEquals(artifact, DownloadManager.stripUrl("wrap:"+artifact));
        assertEquals(artifact, DownloadManager.stripUrl("war:"+artifact));
        assertEquals(artifact, DownloadManager.stripUrl("war:jar:"+artifact));
        assertEquals(artifact, DownloadManager.stripUrl("webbundle:"+artifact));
        assertEquals(artifact, DownloadManager.stripUrl("warref:"+artifact));
        assertEquals(artifact, DownloadManager.stripUrl("war-i:"+artifact));
        assertEquals(artifact, DownloadManager.stripUrl("spring:"+artifact));
        assertEquals(artifact, DownloadManager.stripUrl("blueprint:"+artifact));
    }
}
