function karaf_kill() {
   KARAF_HOME=$1
   INSTANCES_FILE=$KARAF_HOME/instances/instance.properties
   PID=`cat $INSTANCES_FILE | grep "item.0.pid" | awk -F "=" '{print $2}'`
   kill $PID
   wait $PID
}