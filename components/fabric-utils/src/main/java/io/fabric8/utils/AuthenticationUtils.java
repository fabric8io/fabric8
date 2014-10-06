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
package io.fabric8.utils;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.util.Set;

import org.apache.karaf.jaas.boot.principal.UserPrincipal;

/**
 */
public class AuthenticationUtils {

    public static String retrieveJaasUser() {
        Subject subject = Subject.getSubject(AccessController.getContext());
        return retrieveUser(subject);
    }

    public static String retrieveJaasPassword() {
        Subject subject = Subject.getSubject(AccessController.getContext());
        return retrievePassword(subject);
    }


    public static String retrieveUser(Subject subject) {
        if (subject != null &&
                subject.getPrivateCredentials(String.class) != null && !subject.getPrivateCredentials(String.class).isEmpty() &&
                subject.getPrincipals(UserPrincipal.class) != null && !subject.getPrincipals(UserPrincipal.class).isEmpty()) {
            Set<UserPrincipal> userPrincipals = subject.getPrincipals(UserPrincipal.class);
            UserPrincipal userPrincipal = userPrincipals.iterator().next();
            return userPrincipal.getName();
        }
        return null;
    }

    public static String retrievePassword(Subject subject) {
        if (subject != null &&
                subject.getPrivateCredentials(String.class) != null && !subject.getPrivateCredentials(String.class).isEmpty() &&
                subject.getPrincipals(UserPrincipal.class) != null && !subject.getPrincipals(UserPrincipal.class).isEmpty()) {
            return subject.getPrivateCredentials(String.class).iterator().next();
        }
        return null;
    }

}
