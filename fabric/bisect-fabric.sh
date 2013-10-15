#!/bin/sh
# 
# Usage
#
# cp fabric/bisect-fabric.sh bisect-fabric.sh
# git bisect start [badrev] [goodrev]
# git bisect run ./bisect-fabric.sh
#
# @author thomas.diesler@jboss.com
# @since 15-Sep-2013

mvn -ff clean install | tee mvn.out

killall java

MVN_RESULT=`cat mvn.out | grep -o "BUILD SUCCESS\|BUILD FAILURE"`
echo $MVN_RESULT

if [ "$MVN_RESULT" = "BUILD SUCCESS" ]; then
   exit 0
else
   exit 1
fi
