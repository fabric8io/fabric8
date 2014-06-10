#!/bin/bash
# 
# WildFly standalone control script
#

if [ -z "$JBOSS_HOME" ]; then
   echo "JBOSS_HOME not set"
   export JBOSS_HOME=.
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

if [ -z "$JBOSS_CONFIG" ]; then
  JBOSS_CONFIG=standalone/configuration/standalone.xml
fi

JBOSS_SCRIPT=$JBOSS_HOME/bin/standalone.sh

start() {

  #echo JBOSS_PIDFILE=$JBOSS_PIDFILE
  #echo JBOSS_SCRIPT=$JBOSS_SCRIPT
  #echo JBOSS_CONFIG=$JBOSS_CONFIG
  
  echo -n "Starting $JBOSS_PROG_NAME: "

  if [ -f $JBOSS_PIDFILE ]; then
    read ppid < $JBOSS_PIDFILE
    if [ `ps --pid $ppid 2> /dev/null | grep -c $ppid 2> /dev/null` -eq '1' ]; then
      echo -n "$JBOSS_PROG_NAME is already running"
      return 1
    else
      rm -f $JBOSS_PIDFILE
    fi
  fi
  
  bash -c "LAUNCH_JBOSS_IN_BACKGROUND=1 JBOSS_PIDFILE=$JBOSS_PIDFILE $JBOSS_SCRIPT -c $JBOSS_CONFIG" 2>&1 &
  
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