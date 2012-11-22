package org.fusesource.fabric.utils.shell;

import java.io.IOException;
import org.apache.felix.service.command.CommandSession;

public class ShellUtils {

    private ShellUtils() {
        //Utility Class
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
}
