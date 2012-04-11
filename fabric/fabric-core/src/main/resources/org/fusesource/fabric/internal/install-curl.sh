function install_curl() {
  if which curl &> /dev/null; then
    sudo echo "Curl is already installed."
  else
    if which dpkg &> /dev/null; then
      sudo apt-get update
      sudo apt-get -y install curl
    elif which rpm &> /dev/null; then
      yum -y install curl
    fi
  fi
}
