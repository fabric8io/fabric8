# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

$provisionScript = <<SCRIPT

# add host
echo 172.28.128.4 vagrant-ubuntu-trusty-64 >> /etc/hosts

# update limits
echo >> /etc/security/limits.conf
echo *        hard    nproc           8192 >> /etc/security/limits.conf
echo *        soft    nproc           8192 >> /etc/security/limits.conf
echo *        hard    nofile           8192 >> /etc/security/limits.conf
echo *        soft    nofile           8192 >> /etc/security/limits.conf

# add some user aliases
echo >> ~/.bashrc
echo 'export DOCKER_IP=172.28.128.4' >> ~/.bashrc
echo 'alias kube="docker run --rm -i --net=host openshift/origin kube"' >> ~/.bashrc

SCRIPT

# Switch on docker remote API access (without SSL, on port 2375)
$postDockerInstall = <<SCRIPT
echo 'DOCKER_OPTS="${DOCKER_OPTS} -H unix://var/run/docker.sock -H tcp://0.0.0.0:2375 --insecure-registry 172.0.0.0/8"' >> /etc/default/docker
restart docker
sleep 3
docker start openshift
SCRIPT

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "ubuntu/trusty64"

  config.vm.network "private_network", ip: "172.28.128.4"

  config.vm.provider "virtualbox" do |v|
    v.memory = 8192
    v.cpus = 4
#        config.ssh.pty= true

    v.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
  end

  config.vm.provision "shell", inline: $provisionScript

  config.vm.provision "shell", inline: "apt-get update"
  config.vm.provision "docker" do |d|
    d.run "openshift",
      image: "openshift/origin:latest",
      cmd: "start",
      args: "-v /var/run/docker.sock:/var/run/docker.sock --privileged --net=host"
  end
  config.vm.provision "shell", inline: $postDockerInstall
end
