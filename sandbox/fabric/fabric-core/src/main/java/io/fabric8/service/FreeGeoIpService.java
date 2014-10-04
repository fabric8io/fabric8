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
package io.fabric8.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.api.GeoLocationService;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

@Component(name = "io.fabric8.geolocation.freegoip", immediate = true)
@Service(GeoLocationService.class)
public class FreeGeoIpService implements GeoLocationService{

    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";

    @Override
    public String getGeoLocation() {
        String result = "";

        Closeable closeable = null;

        try {
            String urlStr = "http://freegeoip.net/json/";
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

            if (temp.length() > 0) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readValue(temp.toString(), JsonNode.class);
                JsonNode latitudeNode = node.get(LATITUDE);
                JsonNode longitudeNode = node.get(LONGITUDE);
                if (latitudeNode != null && longitudeNode != null) {
                    result = latitudeNode.toString() + "," + longitudeNode.toString();
                }
            }
        } catch (Exception e) {
            //this is going to fail if using this offline
        } finally {
            if (closeable != null) {
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
