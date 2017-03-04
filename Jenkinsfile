#!/usr/bin/groovy
@Library('github.com/fabric8io/fabric8-pipeline-library@master')
def dummy
mavenNode{
    checkout scm
    container(name: 'maven') {
      sh 'chmod 600 /root/.ssh-git/ssh-key'
      sh 'chmod 600 /root/.ssh-git/ssh-key.pub'
      sh 'chmod 700 /root/.ssh-git'
        
      sh "git config user.email fabric8-admin@googlegroups.com"
      sh "git config user.name fabric8-release"

      sh 'mvn package -U'
      sh './update-website.sh'
    }
}
