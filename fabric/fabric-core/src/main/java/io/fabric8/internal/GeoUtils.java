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
package io.fabric8.internal;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 */
public class GeoUtils {
    
    private GeoUtils() {
        //Utils class
    }

    
    /**
     * Get the geographical location (latitude,longitude)
     * @return  String containing the geolocation - or an empty string on failure
     */
    public static String getGeoLocation() {
        final String LATITUDE = "latitude";
        final String LONGITUDE = "longitude";
        String result = "";

        Closeable closeable = null;
        
        try {
            String urlStr =  "http://freegeoip.net/json/";
            URL url = new URL(urlStr);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(50000);
            urlConnection.setReadTimeout(5000);
            InputStream is = urlConnection.getInputStream();
            closeable = is;
            InputStreamReader isr = new InputStreamReader(is);
            closeable = isr;
            BufferedReader in = new BufferedReader(isr);
            closeable = in;
            String inputLine;
            StringBuilder temp = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                temp.append(inputLine);
            }

            if (temp.length() > 0){
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readValue(temp.toString(),JsonNode.class);
                JsonNode latitudeNode = node.get(LATITUDE);
                JsonNode longitudeNode = node.get(LONGITUDE);
                if (latitudeNode != null && longitudeNode != null){
                    result = latitudeNode.toString() + "," + longitudeNode.toString();
                }
            }
        }catch(Exception e) {
            //this is going to fail if using this offline
        }finally {
            if(closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return result;
    }
}
