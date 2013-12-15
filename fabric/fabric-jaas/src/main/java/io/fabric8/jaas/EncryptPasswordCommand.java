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
package io.fabric8.jaas;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.jasypt.util.password.StrongPasswordEncryptor;

@Command(name = "encrypt", scope = "jasypt", description = "Encrypt Password")
public class EncryptPasswordCommand extends OsgiCommandSupport {

    private StrongPasswordEncryptor encryptor = new StrongPasswordEncryptor();

    @Argument(index = 0, name = "password", description = "Password to encrypt", required = true, multiValued = false)
    private String password = null;


    @Override
    protected Object doExecute() throws Exception {
        //TODO allow input of masked passwords

        if (password != null && !password.isEmpty()) {
            System.out.println("Encrypted password: (ENC)" + encryptor.encryptPassword(new String(password)));
        }
        return null;
    }
}
