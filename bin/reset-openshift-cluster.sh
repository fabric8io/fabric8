#!/bin/bash

if [[ $* == *-f* ]]; then
  vagrant reload
fi

echo "Stopping all OpenShift services"
vagrant ssh master -- sudo systemctl stop openshift-master.service
vagrant ssh minion-1 -- sudo systemctl stop openshift-node.service
vagrant ssh minion-2 -- sudo systemctl stop openshift-node.service

echo "Stopping all running Docker containers"
for m in master minion-1 minion-2; do
  vagrant ssh $m -- docker rm -f \$\(docker ps -qa\) \> /dev/null 2\>\&1
done

echo "Clearing all the temporary files"
for m in master minion-1 minion-2; do
  vagrant ssh $m -- sudo rm -rf /openshift.local.*
done

echo "Starting all OpenShift services"
vagrant ssh master -- sudo systemctl start openshift-master.service
vagrant ssh minion-1 -- sudo systemctl start openshift-node.service
vagrant ssh minion-2 -- sudo systemctl start openshift-node.service

echo "Done!"

