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
package org.fusesource.patch.client;

import java.io.File;

import org.fusesource.patch.impl.Offline;

public class Main {

    private static void help() {
        System.out.println("Usage: bin/patch patch-file [karaf-base]");
        System.out.println("   patch-file: location of patch file to apply");
        System.out.println("   karaf-base: directory of the karaf installation");
    }

    public static void main(String[] args) throws Exception {
        File patch;
        File base;

        if (args.length < 1) {
            help();
            return;
        }
        patch = new File(args[0]);
        if (!patch.isFile()) {
            System.err.println("Invalid patch file");
            return;
        }
        if (args.length > 1) {
            base = new File(args[1]);
        } else {
            base = new File(System.getProperty("karaf.base"));
        }
        if (!new File(base, "system").isDirectory() || !new File(base, "etc").isDirectory()) {
            System.err.println("Invalid karaf-base parameter");
            return;
        }

        new Offline(base).apply(patch);
    }

}
