function validate_requirements() {
  if ! which curl &> /dev/null; then
    echo "Command Failed:Curl is not installed.";
  fi
  if ! which java &> /dev/null; then
      echo "Command Failed:Java is not installed.";
      exit -1;
  else
    check_java_version
  fi
  if ! which telnet &> /dev/null; then
        echo "Command Failed:Telnet is not installed.";
        exit -1;
  fi
}

function check_java_version() {
  JAVA_VERSION=`java -version 2>&1 | grep "java version" | awk '{print $3}' | tr -d \" | awk '{split($0, array, ".")} END{print array[2]}'`
  if [ $JAVA_VERSION -ge 6 ]; then
    echo "Java version is greater than 1.6."
  else
    echo "Command Failed:Unsupported java version: 1.$JAVA_VERSION.x found."
    exit -1;
  fi
}
