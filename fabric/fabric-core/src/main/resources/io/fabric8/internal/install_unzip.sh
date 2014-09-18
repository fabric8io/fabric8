function install_unzip() {
  echo "Checking if unzip is present."
  if which unzip &> /dev/null; then
    echo "Unzip is already installed."
  else
    echo "Installing unzip."
    if which dpkg &> /dev/null; then
      sudo_n apt-get -y install unzip
    elif which rpm &> /dev/null; then
     sudo_n yum -y install unzip
    fi
  fi
}
