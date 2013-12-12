package io.fabric8.utils;

/**
 * @author Stan Lewis
 */
public interface Constants {

    String DEBUG_CONTAINER =" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005";
    String PROTOCOL = "fabric.container.protocol";
}
