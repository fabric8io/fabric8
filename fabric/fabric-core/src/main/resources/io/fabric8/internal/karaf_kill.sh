function karaf_kill() {
   KARAF_HOME=$1
   INSTANCES_FILE=$KARAF_HOME/instances/instance.properties
   PID=`cat $INSTANCES_FILE | grep "item.0.pid" | awk -F "=" '{print $2}'`
   $KARAF_HOME/bin/stop
      for i in {1..20};
        do
            if ps -p $PID > /dev/null; then
                echo "Fabric has been successfully stopped"
                break
            else
                sleep 3
            fi
        done
      kill -9 $PID
}
