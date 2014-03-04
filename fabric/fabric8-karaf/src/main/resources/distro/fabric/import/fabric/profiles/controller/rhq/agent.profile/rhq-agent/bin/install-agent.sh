#!/bin/sh
java -jar ./install.bin --install --quiet --launch=false
cat rhq-agent/conf/fabric-agent-configuration.xml | sed '/__default__/D' > rhq-agent/conf/fabric-agent-configuration.xml.new
mv rhq-agent/conf/fabric-agent-configuration.xml.new rhq-agent/conf/fabric-agent-configuration.xml
./rhq-agent/bin/rhq-agent.sh --cleanconfig --nostart --daemon --config fabric-agent-configuration.xml