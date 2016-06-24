#!/bin/sh

# ================================================
# Simple startup script for flat classpath apps
script_dir=`dirname "$0"`

# Discover JAVA_APP_DIR from the script's location.
if [ x"${JAVA_APP_DIR}" = x ] ; then
  JAVA_APP_DIR=`cd "$script_dir"/.. ; pwd`
  export JAVA_APP_DIR
fi

# Custom configuration used by the startup script
if [ -f "${script_dir}/setenv.sh" ] ; then
    source "${script_dir}/setenv.sh"
fi

# Setup main class
main_class=""
if [ x"${JAVA_MAIN_CLASS}" != x ]; then
  main_class="${JAVA_MAIN_CLASS}"
elif [ x"${hawtapp.mvn.main.property}" != x ]; then
  # If given, interpolated by the plugin
  main_class="${hawtapp.mvn.main.property}"
else
  echo "No JAVA_MAIN_CLASS specified"
  exit 1
fi

# Read in classpath
if [ x"${JAVA_CLASSPATH}" != x ]; then
    classpath="${JAVA_CLASSPATH}"
else
    classpath=""
    while read file; do
        classpath="${classpath}:${JAVA_APP_DIR}/lib/$file"
    done < ${JAVA_APP_DIR}/lib/classpath
fi

# Set debug options if required
if [ x"${JAVA_ENABLE_DEBUG}" != x ] && [ "${JAVA_ENABLE_DEBUG}" != "false" ]; then
    java_debug_args="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${JAVA_DEBUG_PORT:-5005}"
fi

# Start application
echo "Launching application in folder: $JAVA_APP_DIR"
arg_list="${exec_args} java ${java_debug_args} ${JAVA_OPTIONS} -classpath ${classpath} ${main_class}"
if [ x"${JAVA_MAIN_ARGS}" != x ] ; then
    arg_list="${arg_list} ${JAVA_MAIN_ARGS}"
else
    arg_list="${arg_list} $@"
fi
echo "Running ${arg_list}"
cd ${JAVA_APP_DIR}
exec ${arg_list}
