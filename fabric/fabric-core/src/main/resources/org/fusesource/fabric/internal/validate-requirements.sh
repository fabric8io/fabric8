function validate-requirements() {
  if ! which curl &> /dev/null; then
    echo "Command Failed:Curl is not installed.";
  fi
  if ! which java &> /dev/null; then
      echo "Command Failed:Java is not installed.";
  fi
  if ! which tar &> /dev/null; then
        echo "Command Failed:Tar is not installed.";
   fi
}