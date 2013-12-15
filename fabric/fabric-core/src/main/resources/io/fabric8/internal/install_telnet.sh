function install_telnet() {
  echo "Checking if telnet is present."
  if which telnet &> /dev/null; then
    echo "Telnet is already installed."
  else
    echo "Installing telnet."
    if which dpkg &> /dev/null; then
      sudo -n apt-get -y install telnet
    elif which rpm &> /dev/null; then
      sudo -n yum -y install telnet
    fi
  fi
}
