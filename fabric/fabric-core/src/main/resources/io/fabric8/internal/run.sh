function run { echo "Running: $*" ; $* ; rc=$? ; if [ "${rc}" -ne 0 ]; then echo "Command Failed:Error running installation script: $*" ; exit ${rc} ; fi ; }
