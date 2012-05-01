function configure_hostnames() {
  CLOUD_PROVIDER=$1
  case $CLOUD_PROVIDER in
    aws-ec2 )
      echo "Resovling public hostname for ec2 node"
      HOSTNAME=`curl http://169.254.169.254/latest/meta-data/public-hostname`
      echo $HOSTNAME
    ;;
    cloudservers | cloudservers-uk | cloudservers-us )
      echo "Resovling public hostname for rackspace node"
      PRIVATE_IP=`/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}'`
      HOSTNAME=`echo $PRIVATE_IP | tr . -`.static.cloud-ips.com
    ;;
  esac
   if [ ! -z "HOSTNAME" ]; then
        LOOKUP_ADDRESS=`nslookup $HOSTNAME > /dev/null | grep Address | tail -n 1 | cut -d " " -f 3`
        echo "Found hostname: $HOSTNAME matching with address: $LOOKUP_ADDRESS"
        echo "publichostname=$HOSTNAME" >> etc/system.properties
        cat etc/system.properties | grep -v 'local.resolver=' | grep -v 'global.resolver=' > etc/system.properties.tmp
        mv etc/system.properties.tmp etc/system.properties
        echo "local.resolver=publichostname" >> etc/system.properties
        echo "global.resolver=publichostname" >> etc/system.properties
        echo $HOSTNAME > hostname
        sudo cp hostname /etc/
        export JAVA_OPTS="-Djava.rmi.server.hostname=$HOSTNAME $JAVA_OPTS"
        echo "RESOLVER OVERRIDE:publichostname"
   fi
}