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
package io.fabric8.gateway.handlers.detecting.protocol.openwire.support.blob;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import io.fabric8.gateway.handlers.detecting.protocol.openwire.command.ActiveMQBlobMessage;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.OpenwireException;

/**
 * A helper class to represent a required upload of a BLOB to some remote URL
 * 
 * @version $Revision: $
 */
public class BlobUploader {

    private BlobTransferPolicy blobTransferPolicy;
    private File file;
    private InputStream in;

    public BlobUploader(BlobTransferPolicy blobTransferPolicy, InputStream in) {
        this.blobTransferPolicy = blobTransferPolicy;
        this.in = in;
    }

    public BlobUploader(BlobTransferPolicy blobTransferPolicy, File file) {
        this.blobTransferPolicy = blobTransferPolicy;
        this.file = file;
    }

    public URL upload(ActiveMQBlobMessage message) throws OpenwireException, IOException {
        if (file != null) {
            return getStrategy().uploadFile(message, file);
        } else {
            return getStrategy().uploadStream(message, in);
        }
    }

    public BlobTransferPolicy getBlobTransferPolicy() {
        return blobTransferPolicy;
    }

    public BlobUploadStrategy getStrategy() {
        return getBlobTransferPolicy().getUploadStrategy();
    }
}
