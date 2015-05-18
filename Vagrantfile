# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "jimmidyson/centos-7.1"
  config.vm.box_version = "= 1.0.0"

  config.vm.network "private_network", ip: "172.28.128.4"

  config.vm.hostname = "fabric8-master.vagrant.local"

  config.vm.provider "virtualbox" do |v|
    v.memory = 8192
    v.cpus = 4

    v.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
  end
end
