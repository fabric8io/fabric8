/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.fuse.examples.cxf.jaxws.client;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * A Java client application that uses a plain HTTP URL connection to send a SOAP request and afterwards receive the
 * SOAP response.
 */
public class Client{

    /**
     * We configured Maven's exec-maven-plugin to be able to run this main method using 'mvn exec:java'
     */
    public static void main(String[] args) {
        try {
        new Client().sendRequest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void sendRequest() throws Exception {
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
        InputStream fis = Client.class.getResourceAsStream("request.xml");
        copyInputStream(fis, os);

        /*
         * ... and afterwards, we just read the SOAP response message that is sent back by the server.
         */
        InputStream is = connection.getInputStream();
	    System.out.println("the response is ====> ");
	    System.out.println(getStringFromInputStream(is));
    }

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

}
