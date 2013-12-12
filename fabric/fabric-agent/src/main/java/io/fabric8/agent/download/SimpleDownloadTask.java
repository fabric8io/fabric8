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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;

public class SimpleDownloadTask extends AbstractDownloadTask {

    public SimpleDownloadTask(String url, ExecutorService executor) {
        super(url, executor);
    }

    @Override
    protected File download() throws Exception {
        File dir = new File(System.getProperty("karaf.data"), "fabric-agent");
        dir.mkdirs();
        File tmpFile = File.createTempFile("download-", null, dir);
        InputStream is = new URL(url).openStream();
        try {
            OutputStream os = new FileOutputStream(tmpFile);
            try {
                copy(is, os);
            } finally {
                os.close();
            }
        } finally {
            is.close();
        }
        return tmpFile;
    }
}
