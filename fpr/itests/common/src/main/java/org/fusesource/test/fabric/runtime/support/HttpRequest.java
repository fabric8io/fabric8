/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.fusesource.test.fabric.runtime.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class HttpRequest {

    public static String get(final String spec, final long timeout, final TimeUnit unit) throws IOException, ExecutionException, TimeoutException {
        final URL url = new URL(spec);
        Callable<String> task = new Callable<String>() {
            @Override
            public String call() throws Exception {
                return processResponse(url, timeout, unit);
            }
        };
        return execute(task, timeout, unit);
    }

    private static String execute(final Callable<String> task, final long timeout, final TimeUnit unit) throws TimeoutException, IOException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<String> result = executor.submit(task);
        try {
            return result.get(timeout, unit);
        } catch (TimeoutException e) {
            result.cancel(true);
            throw e;
        } catch (InterruptedException e) {
            // should not happen
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            // by virtue of the Callable redefinition above I can cast
            throw new IOException(e);
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(timeout, unit);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private static String read(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            out.write(b);
        }
        return out.toString();
    }

    private static String processResponse(URL url, long timeout, TimeUnit unit) throws IOException {
        int responseCode = 0;
        String lastError = "No Error";
        String lastResult = "No Result";
        long now = System.currentTimeMillis();
        long start = System.currentTimeMillis();
        while (responseCode != HttpURLConnection.HTTP_OK && now < start + unit.toMillis(timeout)) {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            try {
                responseCode = con.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    InputStream err = con.getErrorStream();
                    try {
                        lastError = read(err);
                    } finally {
                        err.close();
                    }
                } else {
                    InputStream in = con.getInputStream();
                    try {
                        lastResult = read(in);
                    } finally {
                        in.close();
                    }
                }
            } finally {
                con.disconnect();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                lastError = ex.toString();
                break;
            }
            now = System.currentTimeMillis();
        }
        if (responseCode != HttpURLConnection.HTTP_OK)
            throw new IOException(lastError);

        return lastResult;
    }
}
