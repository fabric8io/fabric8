function update_pkgs() {
  if which dpkg &> /dev/null; then
    sudo -n apt-get update
   elif which rpm &> /dev/null; then
        sudo -n yum check-update
  fi
}
