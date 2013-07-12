function update_pkgs() {
  if which dpkg &> /dev/null; then
    sudo -n sudo apt-get update
  fi
}
