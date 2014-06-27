#!/bin/bash
FABRIC_PIDS=`ps -aef | grep fabric | grep -v kill-fabric |  grep -v grep | awk -F" " {'print $2'}`
if [ "$FABRIC_PIDS" != "" ]
then
   echo "Killing fabric processes: $FABRIC_PIDS"
   kill -9 $FABRIC_PIDS
fi
