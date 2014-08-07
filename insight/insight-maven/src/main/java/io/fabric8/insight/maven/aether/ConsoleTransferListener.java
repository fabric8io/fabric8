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
package io.fabric8.insight.maven.aether;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

public class ConsoleTransferListener extends AbstractTransferListener {

    private PrintStream out;
    private int lastLength = 0;
    private Map<TransferResource, Long> downloads = new HashMap<TransferResource, Long>();

    public ConsoleTransferListener(PrintStream out) {
        this.out = out;
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        this.transferCompleted(event);

        TransferResource resource = event.getResource();
        long contentLength = event.getTransferredBytes();
        if (contentLength >= 0) {
            String message = event.getRequestType().equals(TransferEvent.RequestType.PUT) ? "Uploaded" : "Downloaded";

            String len = contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B";

            String throughput = "";
            long duration = System.currentTimeMillis() - resource.getTransferStartTime();
            if (duration > 0) {
                NumberFormat format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
                double kbPerSec = (contentLength / 1024.0) / (duration / 1000.0);
                throughput = " at " + format.format(kbPerSec) + " KB/sec";
            }

            out.println(message + ": " + resource.getRepositoryUrl() + resource.getResourceName() +
                " (" + len + throughput + ")");
        }
    }

    @Override
    public void transferInitiated(TransferEvent event) throws TransferCancelledException {
        String message = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";

        out.println(message + ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
    }

    @Override
    public void transferProgressed(TransferEvent event) throws TransferCancelledException {
        TransferResource resource = event.getResource();
        downloads.put(resource, event.getTransferredBytes());

        StringBuilder buffer = new StringBuilder(64);

        for (Map.Entry<TransferResource, Long> entry : downloads.entrySet()) {
            long total = entry.getKey().getContentLength();
            long complete = entry.getValue();
            buffer.append(getStatus(complete, total)).append("  ");
        }

        int size = lastLength - buffer.length();
        lastLength = buffer.length();
        pad(buffer, size);
        buffer.append(Aether.LINE_SEPARATOR);

        out.print(buffer);
    }

    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        event.getException().printStackTrace(out);
    }

    @Override
    public void transferFailed(TransferEvent event) {
        transferCompleted(event);

        event.getException().printStackTrace(out);
    }

    private void transferCompleted(TransferEvent event) {
        downloads.remove(event.getResource());

        StringBuilder buffer = new StringBuilder(64);
        pad(buffer, lastLength);
        buffer.append(Aether.LINE_SEPARATOR);
        out.print(buffer);
    }

    private void pad(StringBuilder buffer, int size) {
        String block = "                                        ";
        int spaces = size;
        while (spaces > 0) {
            int n = Math.min(spaces, block.length());
            buffer.append(block.substring(0, n));
            spaces -= n;
        }
    }

    private long toKB(long bytes) {
        return (bytes + 1023) / 1024;
    }

    private String getStatus(long complete, long total) {
        if (total >= 1024) {
            return toKB(complete) + "/" + toKB(total) + " KB ";
        }
        else if (total >= 0) {
            return complete + "/" + total + " B ";
        }
        else if (complete >= 1024) {
            return toKB(complete) + " KB ";
        }
        else {
            return complete + " B ";
        }
    }

}
