function generate_ssh_keys {
  if [ ! -f ~/.ssh/id_rsa ]; then
        mkdir -p ~/.ssh
        ssh-keygen -t rsa -N "" -f ~/.ssh/id_rsa
  fi
}