/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;

import java.util.Arrays;
import java.util.List;

public class ProfileCompleter implements Completer {

    protected FabricService fabricService;

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        String versionName = null;
        Version defaultVersion = fabricService.getDefaultVersion();
        if (defaultVersion != null) {
            versionName = defaultVersion.getName();
        }
        if (versionName == null) {
            versionName = "base";
        }
        Profile[] profiles = fabricService.getProfiles(versionName);
        for (Profile profile : profiles) {
            delegate.getStrings().add(profile.getId());
        }
        return delegate.complete(buffer, cursor, candidates);
    }

}
