function install_curl() {
  echo "Checking if curl is present."
  if which curl &> /dev/null; then
    echo "Curl is already installed."
  else
    echo "Installing curl."
    if which dpkg &> /dev/null; then
      sudo -n apt-get -y install curl
    elif which rpm &> /dev/null; then
     sudo -n yum -y install curl
    fi
  fi
}
