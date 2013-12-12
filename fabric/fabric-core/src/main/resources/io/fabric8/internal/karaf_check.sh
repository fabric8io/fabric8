function karaf_check() {
   KARAF_HOME=$1
   INSTANCES_FILE=$KARAF_HOME/instances/instance.properties
   for i in {1..5};
     do
       if [ ! -f $INSTANCES_FILE ]; then
         sleep 1
       else
         break
       fi
     done
   if [ -f $INSTANCES_FILE ]; then
      for j in {1..5};
         do
           PID=`cat $INSTANCES_FILE | grep "item.0.pid" | awk -F "=" '{print $2}'`
           if [ "$PID" = "" ]; then
             sleep 1
           else
            break
           fi
     done
     if ps -p $PID > /dev/null; then
       echo "Fabric is started successfully"
     else
       echo "Command Failed: Karaf process ($PID) is not running"
     fi
   else
     echo "Command Failed:Could not find Karaf instance.properties"
   fi
}