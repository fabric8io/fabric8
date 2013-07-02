package org.fusesource.fabric.utils;

/**
 * @author Stan Lewis
 */
public interface Constants {

    String DEBUG_CONTAINER =" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005";
    String ENSEMBLE_SERVER_CONTAINER = " -D" + SystemProperties.ENSEMBLE_AUTOSTART + "=true";
    String PROTOCOL = "fabric.container.protocol";
}
