function install_openjdk_deb() {
  sudo apt-get update
  sudo apt-get -y install openjdk-6-jdk
  
  # Try to set JAVA_HOME in a number of commonly used locations
  export JAVA_HOME=/usr/lib/jvm/java-6-openjdk
  if [ -f /etc/profile ]; then
    sudo echo export JAVA_HOME=$JAVA_HOME >> /etc/profile
  fi
  if [ -f /etc/bashrc ]; then
    sudo echo export JAVA_HOME=$JAVA_HOME >> /etc/bashrc
  fi
  if [ -f ~root/.bashrc ]; then
    sudo echo export JAVA_HOME=$JAVA_HOME >> ~root/.bashrc
  fi
  if [ -f /etc/skel/.bashrc ]; then
    sudo echo export JAVA_HOME=$JAVA_HOME >> /etc/skel/.bashrc
  fi
  if [ -f "$DEFAULT_HOME/$NEW_USER" ]; then
    sudo echo export JAVA_HOME=$JAVA_HOME >> $DEFAULT_HOME/$NEW_USER
  fi

  sudo update-alternatives --install /usr/bin/java java $JAVA_HOME/bin/java 17000
  sudo update-alternatives --set java $JAVA_HOME/bin/java
  java -version
}

function install_openjdk_rpm() {
  yes | yum install java-1.6.0-openjdk java-1.6.0-openjdk-devel
  
  # Try to set JAVA_HOME in a number of commonly used locations
  export JAVA_HOME=/usr/lib/jvm/java-1.6.0
  if [ -f /etc/profile ]; then
    echo export JAVA_HOME=$JAVA_HOME >> /etc/profile
  fi
  if [ -f /etc/bashrc ]; then
    echo export JAVA_HOME=$JAVA_HOME >> /etc/bashrc
  fi
  if [ -f ~root/.bashrc ]; then
    echo export JAVA_HOME=$JAVA_HOME >> ~root/.bashrc
  fi
  if [ -f /etc/skel/.bashrc ]; then
    echo export JAVA_HOME=$JAVA_HOME >> /etc/skel/.bashrc
  fi
  if [ -f "$DEFAULT_HOME/$NEW_USER" ]; then
    echo export JAVA_HOME=$JAVA_HOME >> $DEFAULT_HOME/$NEW_USER
  fi

  alternatives --install /usr/bin/java java $JAVA_HOME/bin/java 17000
  alternatives --set java $JAVA_HOME/bin/java
  java -version
}

function install_openjdk() {
    ARCH=`uname -m`
    JAVA_VERSION=`java -version 2>&1`
    if [[ $JAVA_VERSION == *1.6* || $JAVA_VERSION == *1.7* ]]; then
     echo "Java is already installed."
    else
      if which dpkg &> /dev/null; then
        install_openjdk_deb
      elif which rpm &> /dev/null; then
        install_openjdk_rpm
      fi
    fi
}
