#!/bin/bash
# 
# WildFly standalone control script
#

if [ -z "$JBOSS_HOME" ]; then
  JBOSS_HOME=`pwd`
  echo "JBOSS_HOME being set to: $JBOSS_HOME"
fi

if [ -z "$JBOSS_PIDFILE" ]; then
  JBOSS_PIDFILE="data/wildfly.pid"
fi

if [ -z "$JBOSS_PROG_NAME" ]; then
  JBOSS_PROG_NAME="WildFly"
fi

if [ -z "$STARTUP_WAIT" ]; then
  STARTUP_WAIT=30
fi

if [ -z "$SHUTDOWN_WAIT" ]; then
  SHUTDOWN_WAIT=30
fi

if [ -z "$DEBUG_PORT" ]; then
  DEBUG_PORT=8787
fi

# Read an optional running configuration file
if [ -z "$RUN_CONF" ]; then
  RUN_CONF="$JBOSS_HOME/bin/standalone.conf"
fi
if [ -r "$RUN_CONF" ]; then
  bash -c "$RUN_CONF"
fi

# Setup the JVM
if [ -z "$JAVA_HOME" ]; then
  if [ -z "$JAVA" ]; then
    JAVA="java"
  fi
else
  JAVA="$JAVA_HOME/bin/java"
fi

# determine the default base dir, if not set
if [ -z "$JBOSS_BASE_DIR" ]; then
  JBOSS_BASE_DIR="$JBOSS_HOME/standalone"
fi
# determine the default log dir, if not set
if [ -z "$JBOSS_LOG_DIR" ]; then
  JBOSS_LOG_DIR="$JBOSS_BASE_DIR/log"
fi
# determine the default configuration dir, if not set
if [ -z "$JBOSS_CONFIG_DIR" ]; then
  JBOSS_CONFIG_DIR="$JBOSS_BASE_DIR/configuration"
fi

# Set debug settings
if [ "$KARAF_DEBUG" = "true" ]; then
  JAVA_OPTS="$JAVA_OPTS $FABRIC8_JVM_DEBUG_ARGS"
fi

if [ -z "$JBOSS_MODULEPATH" ]; then
  JBOSS_MODULEPATH="$JBOSS_HOME/modules"
fi

start() {

  echo -n "Starting $JBOSS_PROG_NAME."

  if [ -f $JBOSS_PIDFILE ]; then
    read ppid < $JBOSS_PIDFILE
    if [ `ps --pid $ppid 2> /dev/null | grep -c $ppid 2> /dev/null` -eq '1' ]; then
      echo -n "$JBOSS_PROG_NAME is already running"
      return 1
    else
      rm -f $JBOSS_PIDFILE
    fi
  fi

  if [ -n $LAUNCH_JBOSS_IN_BACKGROUND -a $LAUNCH_JBOSS_IN_BACKGROUND = true ]; then
    # Execute the JVM in the background
    eval \"$JAVA\" -D\"[Standalone]\" $JAVA_OPTS \
         \"-Dorg.jboss.boot.log.file=$JBOSS_LOG_DIR/server.log\" \
         \"-Dlogging.configuration=file:$JBOSS_CONFIG_DIR/logging.properties\" \
         -jar \"$JBOSS_HOME/jboss-modules.jar\" \
         -mp \"${JBOSS_MODULEPATH}\" \
         org.jboss.as.standalone \
         -Djboss.home.dir=\"$JBOSS_HOME\" \
         -Djboss.server.base.dir=\"$JBOSS_BASE_DIR\" \
         "$SERVER_OPTS" "&"
    JBOSS_PID=$!
    trap "kill -HUP  $JBOSS_PID" HUP
    trap "kill -TERM $JBOSS_PID" INT
    trap "kill -QUIT $JBOSS_PID" QUIT
    trap "kill -PIPE $JBOSS_PID" PIPE
    trap "kill -TERM $JBOSS_PID" TERM
    if [ -n "$JBOSS_PIDFILE" ]; then
      echo $JBOSS_PID > $JBOSS_PIDFILE
    fi
  else
      # Execute the JVM in the foreground
      eval \"$JAVA\" -D\"[Standalone]\" $JAVA_OPTS \
         \"-Dorg.jboss.boot.log.file=$JBOSS_LOG_DIR/server.log\" \
         \"-Dlogging.configuration=file:$JBOSS_CONFIG_DIR/logging.properties\" \
         -jar \"$JBOSS_HOME/jboss-modules.jar\" \
         -mp \"${JBOSS_MODULEPATH}\" \
         org.jboss.as.standalone \
         -Djboss.home.dir=\"$JBOSS_HOME\" \
         -Djboss.server.base.dir=\"$JBOSS_BASE_DIR\" \
         "$SERVER_OPTS"
      JBOSS_STATUS=$?
  fi

  count=0
  launched=false

  until [ $count -gt $STARTUP_WAIT ]
  do
    if [ -f $JBOSS_PIDFILE ] ; then
      launched=true
      break
    fi
    sleep 1
    let count=$count+1;
    echo -n "."
  done

  echo
  return 0
}

stop() {
  echo -n $"Stopping $JBOSS_PROG_NAME: "
  count=0;

  if [ -f $JBOSS_PIDFILE ]; then
    read kpid < $JBOSS_PIDFILE
    let kwait=$SHUTDOWN_WAIT

    # Try issuing SIGTERM
    kill -15 $kpid
    until [ `ps --pid $kpid 2> /dev/null | grep -c $kpid 2> /dev/null` -eq '0' ] || [ $count -gt $kwait ]
    do
      sleep 1
      let count=$count+1;
      echo -n "."
    done

    if [ $count -gt $kwait ]; then
      kill -9 $kpid
    fi
  fi
  rm -f $JBOSS_PIDFILE
  echo
  return 0
}

forceStop() {
  echo -n $"Force stopping $JBOSS_PROG_NAME: "
  count=0;

  if [ -f $JBOSS_PIDFILE ]; then
    read kpid < $JBOSS_PIDFILE
    let kwait=$SHUTDOWN_WAIT

    # Try issuing SIGKILL
    kill -9 $kpid
    until [ `ps --pid $kpid 2> /dev/null | grep -c $kpid 2> /dev/null` -eq '0' ] || [ $count -gt $kwait ]
    do
      sleep 1
      let count=$count+1;
      echo -n "."
    done

    if [ $count -gt $kwait ]; then
      kill -9 $kpid
    fi
  fi
  rm -f $JBOSS_PIDFILE
  echo
  return 0
}

status() {
  if [ -f $JBOSS_PIDFILE ]; then
    read ppid < $JBOSS_PIDFILE
    if [ `ps --pid $ppid 2> /dev/null | grep -c $ppid 2> /dev/null` -eq '1' ]; then
      echo "$JBOSS_PROG_NAME is running (pid $ppid)"
      return 0
    else
      echo "$JBOSS_PROG_NAME dead but pid file exists"
      return 1
    fi
  fi
  echo "$JBOSS_PROG_NAME is not running"
  return 3
}

case "$1" in
  start)
      start
      ;;
  stop)
      stop
      ;;
  kill)
      forceStop
      ;;
  status)
      status
      ;;
  restart)
      $0 stop
      $0 start
      ;;
  *)
      ## If no parameters are given, print which are avaiable.
      echo "Usage: $0 {start|stop|status|restart}"
      exit 1
      ;;
esac
