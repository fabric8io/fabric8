#!/usr/bin/groovy
@Library('github.com/fabric8io/fabric8-pipeline-library@master')

// lets build things locally

def utils = new io.fabric8.Utils()
def flow = new io.fabric8.Fabric8Commands()

def label = "buildpod.${env.JOB_NAME}.${env.BUILD_NUMBER}".replace('-', '_').replace('/', '_')

podTemplate(label: label, serviceAccount: 'jenkins', containers: [
        [name: 'maven', image: 'fabric8/maven-builder', command: 'cat', ttyEnabled: true, envVars: [
                [key: 'TERM', value: 'dumb'], // this removes colour from gofabric8
                [key: 'MAVEN_OPTS', value: '-Duser.home=/home/jenkins/'],
                [key: 'DOCKER_CONFIG', value: '/home/jenkins/.docker/'],
                [key: 'KUBERNETES_MASTER', value: 'kubernetes.default']]],
        [name: 'jnlp', image: 'iocanel/jenkins-jnlp-client:latest', command:'/usr/local/bin/start.sh', args: '${computer.jnlpmac} ${computer.name}', ttyEnabled: false,
                envVars: [[key: 'DOCKER_HOST', value: 'unix:/var/run/docker.sock']]]],
        volumes: [
                [$class: 'PersistentVolumeClaim', mountPath: '/home/jenkins/.mvnrepository', claimName: 'jenkins-mvn-local-repo'],
                [$class: 'SecretVolume', mountPath: '/home/jenkins/.m2/', secretName: 'jenkins-maven-settings'],
                [$class: 'SecretVolume', mountPath: '/home/jenkins/.docker', secretName: 'jenkins-docker-cfg'],
                [$class: 'HostPathVolume', mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock']
        ]) {
  node(label) {
    git 'https://github.com/fabric8io/fabric8.git' 'helm-index-build'

    container(name: 'maven') {
      sh "./update-website.sh"
    }
  }
}
