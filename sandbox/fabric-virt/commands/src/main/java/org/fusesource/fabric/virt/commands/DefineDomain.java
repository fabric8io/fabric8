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

package io.fabric8.virt.commands;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

@Command(scope = "virt", name = "domain-define")
public class DefineDomain extends LibvirtCommandSupport {

    @Argument(name = "descriptorUri", description = "A URI to a domain descriptor", required = true, multiValued = false)
    private String descriptorUri;

    @Override
    protected Object doExecute() throws Exception {
        URI uri = new URI(descriptorUri);
        InputStream is = null;
        BufferedReader reader = null;
        try{
        is = uri.toURL().openStream();
        reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder builder = new StringBuilder();
        for(String line = reader.readLine(); line != null; line = reader.readLine()) {
            builder.append(line);
        }
        String descriptor = builder.toString();
        getConnection().domainDefineXML(descriptor);

        } finally {
            if (reader != null) {
                reader.close();
            }
            if (is != null) {
                is.close();
            }
        }
        return null;
    }
}
