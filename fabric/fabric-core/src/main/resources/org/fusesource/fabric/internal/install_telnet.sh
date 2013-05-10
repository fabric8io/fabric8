function install_telnet() {
  echo "Checking if telnet is present."
  if which telnet &> /dev/null; then
    sudo echo "Telnet is already installed."
  else
    echo "Installing telnet."
    if which dpkg &> /dev/null; then
      sudo apt-get update
      sudo apt-get -y install telnet
    elif which rpm &> /dev/null; then
      yum -y install telnet
    fi
  fi
}
