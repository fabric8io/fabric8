# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

$provisionScript = <<SCRIPT
if [ -d '/var/lib/openshift' ]; then
  exit 0
fi

mkdir /tmp/openshift
echo "Downloading OpenShift binaries..."
curl -sSL https://github.com/openshift/origin/releases/download/v0.5.1/openshift-origin-v0.5.1-ce1e6c4-linux-amd64.tar.gz | tar xzv -C /tmp/openshift
mv /tmp/openshift/* /usr/bin/

mkdir /var/lib/openshift
restorecon -v /var/lib/openshift

cat <<EOF > /usr/lib/systemd/system/openshift.service
[Unit]
Description=OpenShift
Requires=docker.service network.service
After=network.service
[Service]
ExecStart=/usr/bin/openshift start --master=172.28.128.4 --cors-allowed-origins=.*
WorkingDirectory=/var/lib/openshift/
[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable openshift.service
systemctl start openshift.service

mkdir -p ~/.config/openshift
ln -s /var/lib/openshift/openshift.local.config/master/admin.kubeconfig ~/.config/openshift/config

while true; do
  (osc get namespaces default | grep default) > /dev/null 2>&1 && break || sleep 1
done

sleep 30

osadm policy add-cluster-role-to-user cluster-admin admin
osadm router --create --credentials=/var/lib/openshift/openshift.local.config/master/openshift-router.kubeconfig
osadm registry --create --credentials=/var/lib/openshift/openshift.local.config/master/openshift-registry.kubeconfig
osadm new-project fabric8 --description="fabric8 Apps"

cat <<EOF | osc create -f -
---
  apiVersion: "v1beta3"
  kind: "Secret"
  metadata:
    name: "openshift-cert-secrets"
  data:
    root-cert: "$(base64 -w 0 /var/lib/openshift/openshift.local.config/master/ca.crt)"
    admin-cert: "$(base64 -w 0 /var/lib/openshift/openshift.local.config/master/admin.crt)"
    admin-key: "$(base64 -w 0 /var/lib/openshift/openshift.local.config/master/admin.key)"
EOF

# Create route to registry. Please not that this route can be used for applications to find
# the images. Since it goes through the router this is not optimal for production setups
# when internally images are fetched. 
cat <<EOF | osc create -f -
{
    "kind": "Route",
    "apiVersion": "v1beta3",
    "metadata": {
        "name": "docker-registry-route"
    },
    "spec": {
        "host": "docker-registry.vagrant.local",
        "to": {
            "kind": "Service",
            "name": "docker-registry"
        }
    }
}
EOF
echo "127.0.0.1    docker-registry.vagrant.local" >> /etc/hosts

SCRIPT

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "jimmidyson/centos-7.1"
  config.vm.box_version = "= 1.0.1"

  config.vm.network "private_network", ip: "172.28.128.4"

  config.vm.hostname = "fabric8-master.vagrant.local"

  config.vm.provider "virtualbox" do |v|
    v.memory = 4096
    v.cpus = 2
    v.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
  end

  config.vm.provision "shell", inline: $provisionScript

end
