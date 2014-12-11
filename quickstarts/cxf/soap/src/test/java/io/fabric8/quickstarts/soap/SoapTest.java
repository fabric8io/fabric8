/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.quickstarts.soap;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * A Java client application that uses a plain HTTP URL connection to send a SOAP request and afterwards receive the
 * SOAP response.
 */
public class SoapTest {

    private static final Logger LOG = LoggerFactory.getLogger(SoapTest.class);

    /**
     * Helper method to copy bytes from an InputStream to an OutputStream.
     */
    private static void copyInputStream(InputStream in, OutputStream out) throws Exception {
        int c = 0;
        try {
            while ((c = in.read()) != -1) {
                out.write(c);
            }
        } finally {
            in.close();
        }
    }

    /**
     * Helper method to read bytes from an InputStream and return them as a String.
     */
    private static String getStringFromInputStream(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        copyInputStream(in, bos);
        bos.close();
        return bos.toString();
    }

    @Test
    public void sendRequest() throws Exception {

        String res;
        /*
         * Set up the URL connection to the web service address
         */
        URLConnection connection = new URL("http://localhost:8181/cxf/HelloWorld").openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);

        /*
         * We have prepared a SOAP request in an XML file, so we send the contents of that file to our web service...
         */
        OutputStream os = connection.getOutputStream();
        InputStream fis = SoapTest.class.getResourceAsStream("/request.xml");
        copyInputStream(fis, os);

        /*
         * ... and afterwards, we just read the SOAP response message that is sent back by the server.
         */
        InputStream is = connection.getInputStream();
        LOG.info("the response is ====> ");
        res = getStringFromInputStream(is);
        LOG.info(res);
        Assert.assertTrue(res.contains("Hello"));
    }

}
