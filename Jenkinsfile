#!/usr/bin/groovy
@Library('github.com/fabric8io/fabric8-pipeline-library@master')
def dummy
mavenNode{
    git uri: 'https://github.com/fabric8io/fabric8.git', branch: 'helm-index-build'

    container(name: 'maven') {
      sh 'mvn package -U'
      sh './update-website.sh'
    }
}
