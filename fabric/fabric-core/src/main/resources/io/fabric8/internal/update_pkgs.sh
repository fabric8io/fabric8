function update_pkgs() {
  if which dpkg &> /dev/null; then
    sudo_n apt-get update
   elif which rpm &> /dev/null; then
        sudo_n yum check-update
  fi
}
