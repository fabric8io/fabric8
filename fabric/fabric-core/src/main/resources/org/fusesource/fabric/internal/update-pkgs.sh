function update-pkgs() {
  if which dpkg &> /dev/null; then
    sudo apt-get update
  fi
}
