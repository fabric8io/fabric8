#!/bin/bash
KARAF_PIDS=`ps -aef | grep karaf | grep -v kill-karaf |  grep -v grep | awk -F" " {'print $2'}`
if [ "$KARAF_PIDS" != "" ]
then
echo "Killing karaf processes: $KARAF_PIDS"
kill -9 $KARAF_PIDS
fi
