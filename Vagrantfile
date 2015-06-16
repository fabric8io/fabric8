# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

$provisionScript = <<SCRIPT
if [ -d '/var/lib/openshift' ]; then
  exit 0
fi

yum remove -y docker-logrotate
yum install -y http://cbs.centos.org/kojifiles/packages/docker/1.6.2/3.gitc3ca5bb.el7/x86_64/docker-1.6.2-3.gitc3ca5bb.el7.x86_64.rpm

systemctl restart docker.service

mkdir /tmp/openshift
echo "Downloading OpenShift binaries..."
curl -sSL https://github.com/openshift/origin/releases/download/v0.6.1/openshift-origin-v0.6.1-160d4b6-linux-amd64.tar.gz | tar xzv -C /tmp/openshift
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

mkdir -p ~/.kube/
ln -s /var/lib/openshift/openshift.local.config/master/admin.kubeconfig ~/.kube/config

while true; do
  curl -k -s -f -o /dev/null --connect-timeout 1 https://localhost:8443/healthz/ready && break || sleep 1
done

sleep 30

oadm policy add-cluster-role-to-user cluster-admin admin
oadm router --create --credentials=/var/lib/openshift/openshift.local.config/master/openshift-router.kubeconfig
oadm registry --create --credentials=/var/lib/openshift/openshift.local.config/master/openshift-registry.kubeconfig
oadm new-project fabric8 --description="fabric8 Apps"

cat <<EOF | oc create -f -
---
  apiVersion: "v1"
  kind: "Secret"
  metadata:
    name: "openshift-cert-secrets"
  data:
    root-cert: "$(base64 -w 0 /var/lib/openshift/openshift.local.config/master/ca.crt)"
    admin-cert: "$(base64 -w 0 /var/lib/openshift/openshift.local.config/master/admin.crt)"
    admin-key: "$(base64 -w 0 /var/lib/openshift/openshift.local.config/master/admin.key)"
EOF
cat <<EOF | oc create -f -
---
  apiVersion: v1
  kind: ServiceAccount
  metadata:
    name: fabric8
  secrets:
    -
      name: openshift-cert-secrets
EOF

# Create route to registry. Please not that this route can be used for applications to find
# the images. Since it goes through the router this is not optimal for production setups
# when internally images are fetched. 
cat <<EOF | oc create -f -
{
    "kind": "Route",
    "apiVersion": "v1",
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
  config.vm.box_version = "= 1.0.2"

  config.vm.network "private_network", ip: "172.28.128.4"

  config.vm.hostname = "fabric8-master.vagrant.local"

  config.vm.provider "virtualbox" do |v|
    v.memory = 4096
    v.cpus = 2
    v.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
  end

  config.vm.provision "shell", inline: $provisionScript

end
