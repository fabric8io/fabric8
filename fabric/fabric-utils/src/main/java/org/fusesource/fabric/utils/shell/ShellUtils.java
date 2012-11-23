package org.fusesource.fabric.utils.shell;

import java.io.IOException;
import java.security.AccessController;
import java.util.Set;
import javax.security.auth.Subject;
import jline.Terminal;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;

public class ShellUtils {

    private static final String FABRIC_USER = "FABRIC_USER";
    private static final String FABRIC_USER_PASSWORD = "FABRIC_USER_PASSWORD";

    private ShellUtils() {
        //Utility Class
    }

    /**
     * Stores username and password to the {@link CommandSession}.
     * @param session
     * @param username
     * @param password
     */
    public static void storeFabricCredentials(CommandSession session, String username, String password) {
        session.put(FABRIC_USER, username);
        session.put(FABRIC_USER_PASSWORD, password);
    }

    /**
     * Returns the fabric username stored in the {@link CommandSession}.
     * @param session
     * @return
     */
    public static String retrieveFabricUser(CommandSession session) {
        Subject subject = Subject.getSubject(AccessController.getContext());
        if (!subject.getPrivateCredentials(String.class).isEmpty() && subject.getPrincipals(UserPrincipal.class) != null && !subject.getPrincipals(UserPrincipal.class).isEmpty()) {
            Set<UserPrincipal> userPrincipals = subject.getPrincipals(UserPrincipal.class);
            UserPrincipal userPrincipal = userPrincipals.iterator().next();
            return userPrincipal.getName();
        }

        if (session != null && session.get(FABRIC_USER) != null) {
            return (String) session.get(FABRIC_USER);
        }
        return null;
    }


    /**
     * Returns the fabric username stored in the {@link CommandSession}.
     * @param session
     * @return
     */
    public static String retrieveFabricUserPassword(CommandSession session) {
        Subject subject = Subject.getSubject(AccessController.getContext());
        if (!subject.getPrivateCredentials(String.class).isEmpty() && !subject.getPrincipals(UserPrincipal.class).isEmpty()) {
            return subject.getPrivateCredentials(String.class).iterator().next();
        }
        if (session != null && session.get(FABRIC_USER_PASSWORD) != null) {
            return (String) session.get(FABRIC_USER_PASSWORD);
        }
        return null;
    }


    /**
     * Displays the message and reads the input.
     * @param session   The {@link CommandSession} to use.
     * @param msg       The message to display.
     * @param hidden    Flag to hide the user input.
     * @return
     * @throws IOException
     */
    public static String readLine(CommandSession session, String msg, boolean hidden) throws IOException {
        StringBuffer sb = new StringBuffer();
        System.out.print(msg);
        System.out.flush();
        for (; ; ) {
            int c = session.getKeyboard().read();
            if (c < 0) {
                return null;
            }

            if (!hidden) {
                System.out.print((char) c);
                System.out.flush();
            }
            if (c == '\r' || c == '\n') {
                break;
            }
            sb.append((char) c);
        }
        return sb.toString();
    }

    /**
     * Returns the {@Terminal} width.
     * @param session
     * @return
     */
    public static int getTermWidth(CommandSession session) {
        Terminal term = (Terminal) session.get(".jline.terminal");
        return term != null ? term.getWidth() : 80;
    }
}
