#!/usr/bin/groovy
@Library('github.com/fabric8io/fabric8-pipeline-library@master')
def dummy
mavenNode{
    checkout scm
    container(name: 'maven') {
      sh 'mvn package -U'
      sh './update-website.sh'
    }
}
