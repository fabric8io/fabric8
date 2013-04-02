function install_unzip() {
  echo "Checking if unzip is present."
  if which unzip &> /dev/null; then
    sudo echo "Unzip is already installed."
  else
    echo "Installing unzip."
    if which dpkg &> /dev/null; then
      sudo apt-get update
      sudo apt-get -y install unzip
    elif which rpm &> /dev/null; then
      yum -y install unzip
    fi
  fi
}
