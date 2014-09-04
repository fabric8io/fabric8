function sudo_n {
SUDO_NON_INTERACTIVE=`sudo -h | grep "\-n"`
  if [ -z "$SUDO_NON_INTERACTIVE" ]; then
     sudo $*
  else
     sudo -n $*
  fi
}
