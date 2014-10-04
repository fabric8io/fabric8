function install_openjdk_deb() {
  sudo_n apt-get -y install openjdk-7-jdk
  
  # Try to set JAVA_HOME in a number of commonly used locations
  # Lifting JAVA_HOME detection from jclouds
      for CANDIDATE in `ls -d /usr/lib/jvm/java-1.7.0-openjdk-* /usr/lib/jvm/java-7-openjdk-* /usr/lib/jvm/java-7-openjdk 2>&-`; do
          if [ -n "$CANDIDATE" -a -x "$CANDIDATE/bin/java" ]; then
              export JAVA_HOME=$CANDIDATE
              break
          fi
      done

  if [ -f /etc/profile ]; then
    sudo_n "echo 'export JAVA_HOME=$JAVA_HOME' >> /etc/profile"
  fi
  if [ -f /etc/bashrc ]; then
    sudo_n "echo 'export JAVA_HOME=$JAVA_HOME' >> /etc/bashrc"
  fi
  if [ -f ~root/.bashrc ]; then
    sudo_n "echo 'export JAVA_HOME=$JAVA_HOME' >> ~root/.bashrc"
  fi
  if [ -f /etc/skel/.bashrc ]; then
    sudo_n "echo 'export JAVA_HOME=$JAVA_HOME' >> /etc/skel/.bashrc"
  fi
  if [ -f "$DEFAULT_HOME/$NEW_USER" ]; then
    sudo_n "echo 'export JAVA_HOME=$JAVA_HOME' >> $DEFAULT_HOME/$NEW_USER"
  fi

  sudo_n update-alternatives --install /usr/bin/java java $JAVA_HOME/bin/java 17000
  sudo_n update-alternatives --set java $JAVA_HOME/bin/java
  java -version
}

function install_openjdk_rpm() {
  sudo_n yum -y install java-1.7.0-openjdk-devel
  
  # Try to set JAVA_HOME in a number of commonly used locations
  # Lifting JAVA_HOME detection from jclouds
      for CANDIDATE in `ls -d /usr/lib/jvm/java-1.7.0-openjdk-* /usr/lib/jvm/java-7-openjdk-* /usr/lib/jvm/java-7-openjdk 2>&-`; do
          if [ -n "$CANDIDATE" -a -x "$CANDIDATE/bin/java" ]; then
              export JAVA_HOME=$CANDIDATE
              break
          fi
      done

  if [ -f /etc/profile ]; then
    sudo_n "echo 'export JAVA_HOME=$JAVA_HOME' >> /etc/profile"
  fi
  if [ -f /etc/bashrc ]; then
    sudo_n "echo 'export JAVA_HOME=$JAVA_HOME' >> /etc/bashrc"
  fi
  if [ -f ~root/.bashrc ]; then
    sudo_n "echo 'export JAVA_HOME=$JAVA_HOME' >> ~root/.bashrc"
  fi
  if [ -f /etc/skel/.bashrc ]; then
    sudo_n "echo 'export JAVA_HOME=$JAVA_HOME' >> /etc/skel/.bashrc"
  fi
  if [ -f "$DEFAULT_HOME/$NEW_USER" ]; then
    sudo_n "echo 'export JAVA_HOME=$JAVA_HOME' >> $DEFAULT_HOME/$NEW_USER"
  fi

  sudo_n alternatives --install /usr/bin/java java $JAVA_HOME/bin/java 17000
  sudo_n alternatives --set java $JAVA_HOME/bin/java
  java -version
}

function install_openjdk() {
    echo "Checking if java is present."
    ARCH=`uname -m`
    JAVA_VERSION=`java -version 2>&1`
    if [[ $JAVA_VERSION == *1.7* ]]; then
     echo "Java is already installed."
    else
      echo "Installing java."
      if which dpkg &> /dev/null; then
        install_openjdk_deb
      elif which rpm &> /dev/null; then
        install_openjdk_rpm
      fi
    fi
}
