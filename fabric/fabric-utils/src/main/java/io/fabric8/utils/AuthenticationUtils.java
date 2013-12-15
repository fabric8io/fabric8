package io.fabric8.utils;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.util.Set;

import org.apache.karaf.jaas.boot.principal.UserPrincipal;

/**
 * @author Stan Lewis
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
