/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.fabric8.itests.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Thomas.Diesler@jboss.com
 */
public class HttpRequest {

    public static String get(final String spec, final long timeout, final TimeUnit unit) throws IOException {
        return get(spec, null, timeout, unit);
    }

    public static String get(final String spec, final Map<String, String> headers, final long timeout, final TimeUnit unit) throws IOException {
        try {
            Callable<String> task = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return processResponse(new URL(spec), headers, timeout, unit);
                }
            };
            return execute(task, timeout, unit);
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IOException("Error accessing: " + spec, ex);
        }
    }

    private static String execute(final Callable<String> task, final long timeout, final TimeUnit unit) throws TimeoutException, ExecutionException {
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
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(timeout, unit);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private static String processResponse(URL url, Map<String, String> headers, long timeout, TimeUnit unit) throws IOException {
        int responseCode = 0;
        String lastError = "No Error";
        String lastResult = "No Result";
        long now = System.currentTimeMillis();
        long start = System.currentTimeMillis();
        while (responseCode != HttpURLConnection.HTTP_OK && now < start + unit.toMillis(timeout)) {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            if (headers != null) {
                for (Entry<String, String> entry : headers.entrySet()) {
                    con.addRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            con.setDoInput(true);
            try {
                responseCode = con.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    InputStream err = con.getErrorStream();
                    if (err != null) {
                        try {
                            lastError = read(err);
                        } finally {
                            err.close();
                        }
                    } else {
                        InputStream in = con.getInputStream();
                        try {
                            lastError = read(in);
                        } finally {
                            in.close();
                        }
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

    private static String read(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            out.write(b);
        }
        return out.toString();
    }
}
