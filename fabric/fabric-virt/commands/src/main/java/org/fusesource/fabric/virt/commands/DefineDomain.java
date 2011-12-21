/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.virt.commands;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
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
