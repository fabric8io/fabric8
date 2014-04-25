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
 * Represents a strategy of uploading a file/stream to some remote
 *
 * @version $Revision: $
 */
public interface BlobUploadStrategy {

    URL uploadFile(ActiveMQBlobMessage message, File file) throws OpenwireException, IOException;

    URL uploadStream(ActiveMQBlobMessage message, InputStream in) throws OpenwireException, IOException;
}
