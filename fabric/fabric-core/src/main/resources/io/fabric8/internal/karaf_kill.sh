function karaf_kill() {
   KARAF_HOME=$1
   INSTANCES_FILE=$KARAF_HOME/instances/instance.properties
   PID=`cat $INSTANCES_FILE | grep "item.0.pid" | awk -F "=" '{print $2}'`
   kill $PID
      for i in {1..5};
        do
            if ps -p $PID > /dev/null; then
                echo "Fabric has been successfully stopped"
                break
            else
                sleep 3
            fi
        done
}