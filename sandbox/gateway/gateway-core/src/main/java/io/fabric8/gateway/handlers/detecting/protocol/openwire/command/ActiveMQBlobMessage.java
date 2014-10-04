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
package io.fabric8.gateway.handlers.detecting.protocol.openwire.command;

import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.blob.BlobUploader;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.OpenwireException;
import org.fusesource.hawtbuf.UTF8Buffer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @version $Revision: $
 * @openwire:marshaller code="29" version=3
 */
public class ActiveMQBlobMessage extends ActiveMQMessage {
    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.ACTIVEMQ_BLOB_MESSAGE;

    public static final UTF8Buffer BINARY_MIME_TYPE = new UTF8Buffer("application/octet-stream");

    private UTF8Buffer remoteBlobUrl;
    private UTF8Buffer mimeType;
    private UTF8Buffer name;
    private boolean deletedByBroker;

    private transient BlobUploader blobUploader;
    private transient URL url;

    public Message copy() {
        ActiveMQBlobMessage copy = new ActiveMQBlobMessage();
        copy(copy);
        return copy;
    }

    private void copy(ActiveMQBlobMessage copy) {
        super.copy(copy);
        copy.setRemoteBlobUrl(getRemoteBlobUrl());
        copy.setMimeType(getMimeType());
        copy.setDeletedByBroker(isDeletedByBroker());
        copy.setBlobUploader(getBlobUploader());
        copy.setName(getName());
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    /**
     * @openwire:property version=3 cache=false
     */
    public UTF8Buffer getRemoteBlobUrl() {
        return remoteBlobUrl;
    }

    public void setRemoteBlobUrl(UTF8Buffer remoteBlobUrl) {
        this.remoteBlobUrl = remoteBlobUrl;
        url = null;
    }

    /**
     * The MIME type of the BLOB which can be used to apply different content
     * types to messages.
     * 
     * @openwire:property version=3 cache=true
     */
    public UTF8Buffer getMimeType() {
        if (mimeType == null) {
            return BINARY_MIME_TYPE;
        }
        return mimeType;
    }

    public void setMimeType(UTF8Buffer mimeType) {
        this.mimeType = mimeType;
    }

    public UTF8Buffer getName() {
        return name;
    }

    /**
     * The name of the attachment which can be useful information if
     * transmitting files over ActiveMQ
     * 
     * @openwire:property version=3 cache=false
     */
    public void setName(UTF8Buffer name) {
        this.name = name;
    }

    /**
     * @openwire:property version=3 cache=false
     */
    public boolean isDeletedByBroker() {
        return deletedByBroker;
    }

    public void setDeletedByBroker(boolean deletedByBroker) {
        this.deletedByBroker = deletedByBroker;
    }

    public UTF8Buffer getJMSXMimeType() {
        return getMimeType();
    }

    public InputStream getInputStream() throws IOException, OpenwireException {
        URL value = getURL();
        if (value == null) {
            return null;
        }
        return value.openStream();
    }

    public URL getURL() throws OpenwireException {
        if (url == null && remoteBlobUrl != null) {
            try {
                url = new URL(remoteBlobUrl.toString());
            } catch (MalformedURLException e) {
                throw new OpenwireException(e);
            }
        }
        return url;
    }

    public void setURL(URL url) {
        this.url = url;
        remoteBlobUrl = url != null ? new UTF8Buffer(url.toExternalForm()) : null;
    }

    public BlobUploader getBlobUploader() {
        return blobUploader;
    }

    public void setBlobUploader(BlobUploader blobUploader) {
        this.blobUploader = blobUploader;
    }

    public void onSend() throws OpenwireException {
        super.onSend();

        // lets ensure we upload the BLOB first out of band before we send the
        // message
        if (blobUploader != null) {
            try {
                URL value = blobUploader.upload(this);
                setURL(value);
            } catch (IOException e) {
                throw new OpenwireException(e);
            }
        }
    }
}
