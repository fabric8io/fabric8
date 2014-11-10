#!/bin/bash

echo "Restarting the vagrant boxes"
vagrant restart

echo "Clearing all the temporary files"
vagrant ssh -c "sudo rm -rf /openshift.local.*" master
vagrant ssh -c "sudo rm -rf /openshift.local.*" minion-1
vagrant ssh -c "sudo rm -rf /openshift.local.*" minion-2

echo "Restart all the services"
vagrant ssh -c "sudo systemctl start openshift-master.service" master
vagrant ssh -c "sudo systemctl start openshift-node.service" minion-1
vagrant ssh -c "sudo systemctl start openshift-node.service" minion-2

echo "Done!"

