#!/bin/sh
# launch script
#
# 
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_10.jdk/Contents/Home/

DIR=`cd "\`dirname "$0"\`" && pwd`

JAR_DIR="$DIR/../../../target"

if [ -z "$JAVA_HOME" ] ; then
        JAVA_HOME=`readlink -f \`which java 2>/dev/null\` 2>/dev/null | \
        sed 's/\/bin\/java//'`
fi

TOOLSJAR="$JAVA_HOME/lib/tools.jar"

if [ ! -f "$TOOLSJAR" ] ; then
        echo "$JAVA_HOME seems to be no JDK!" >&2
        exit 1
fi

"$JAVA_HOME"/bin/java $JAVA_OPTS -cp "$JAR_DIR/apmagent-project-1.1.0-SNAPSHOT.jar:$TOOLSJAR" \
io.fabric8.apmagent.ApmAgentLauncher "$@"
exit $?
