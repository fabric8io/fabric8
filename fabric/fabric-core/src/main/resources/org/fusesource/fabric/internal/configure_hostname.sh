function configure_hostnames() {
  CLOUD_PROVIDER=$1
  case $CLOUD_PROVIDER in
    openstack-nova | ec2 | aws-ec2 )
      echo "Resolving public hostname for ec2 node"
      export PUBLIC_HOSTNAME=`curl http://169.254.169.254/latest/meta-data/public-hostname | sed 's/ /_/g'`
      echo PUBLIC_HOSTNAME
    ;;
    cloudservers | cloudservers-uk | cloudservers-us )
      echo "Resovling public hostname for rackspace node"
      PRIVATE_IP=`/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}'`
      export PUBLIC_HOSTNAME=`echo $PRIVATE_IP | tr . -`.static.cloud-ips.com
    ;;
  esac
   if [ ! -z ${PUBLIC_HOSTNAME} ]; then
        LOOKUP_ADDRESS=`nslookup $PUBLIC_HOSTNAME > /dev/null | grep Address | tail -n 1 | cut -d " " -f 3 | sed 's/ /_/g'`
        echo "Found hostname: $PUBLIC_HOSTNAME matching with address: $LOOKUP_ADDRESS"
        echo "publichostname=$PUBLIC_HOSTNAME" >> etc/system.properties
        cat etc/system.properties | grep -v 'local.resolver=' | grep -v 'global.resolver=' > etc/system.properties.tmp
        mv etc/system.properties.tmp etc/system.properties
        echo "local.resolver=publichostname" >> etc/system.properties
        echo "global.resolver=publichostname" >> etc/system.properties
        echo $PUBLIC_HOSTNAME > hostname
        sudo -n cp hostname /etc/
        export JAVA_OPTS="-Djava.rmi.server.hostname=$PUBLIC_HOSTNAME $JAVA_OPTS"
        echo "RESOLVER OVERRIDE:publichostname"
   fi
}