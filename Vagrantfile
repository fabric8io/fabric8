# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

$provisionScript = <<SCRIPT

if [ -d 'openshift.local.config' ]; then
  exit 0
fi

curl -sSL https://github.com/openshift/origin/releases/download/v0.5.1/openshift-origin-v0.5.1-ce1e6c4-linux-amd64.tar.gz | tar xzv

mkdir /var/lib/openshift
restorecon -v /var/lib/openshift

nohup ./openshift start \
        --cors-allowed-origins='.*' \
        --master=172.28.128.4 \
        --volume-dir=/var/lib/openshift/openshift.local.volumes \
        --etcd-dir=/var/lib/openshift/openshift.local.etcd \
  > /var/lib/openshift/openshift.log &

mkdir -p ~/.config/openshift
ln -s `pwd`/openshift.local.config/master/admin.kubeconfig ~/.config/openshift/config

while true; do
  (./osc get namespaces default | grep default) && break || sleep 1
done

sleep 30

./osadm policy add-cluster-role-to-user cluster-admin admin
./osadm router --create --credentials=openshift.local.config/master/openshift-router.kubeconfig
./osadm registry --create --credentials=openshift.local.config/master/openshift-registry.kubeconfig
SCRIPT

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "jimmidyson/centos-7.1"
  config.vm.box_version = "= 1.0.1"

  config.vm.network "private_network", ip: "172.28.128.4"

  config.vm.hostname = "fabric8-master.vagrant.local"

  config.vm.provider "virtualbox" do |v|
    v.memory = 8192
    v.cpus = 4
    v.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
  end

  config.vm.provision "shell", inline: $provisionScript

end
