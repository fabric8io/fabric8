#!/usr/bin/groovy
def updateDependencies(source){

  def properties = []
  properties << ['<kubernetes-client.version>','io/fabric8/kubernetes-client']
  properties << ['<docker.maven.plugin.version>','io/fabric8/docker-maven-plugin']
  properties << ['<sundrio.version>','io/sundr/sundr-maven-plugin']

  updatePropertyVersion{
    updates = properties
    repository = source
    project = 'fabric8io/fabric8'
  }
}

def stage(){
  return stageProject{
    project = 'fabric8io/fabric8'
    useGitTagForNextVersion = true
  }
}

def approveRelease(project){
  def releaseVersion = project[1]
  approve{
    room = null
    version = releaseVersion
    console = null
    environment = 'fabric8'
  }
}

def release(project){
  releaseProject{
    stagedProject = project
    useGitTagForNextVersion = true
    helmPush = false
    groupId = 'io.fabric8'
    githubOrganisation = 'fabric8io'
    artifactIdToWatchInCentral = 'kubernetes-api'
    artifactExtensionToWatchInCentral = 'jar'
  }
}

def mergePullRequest(prId){
  mergeAndWaitForPullRequest{
    project = 'fabric8io/fabric8'
    pullRequestId = prId
  }
}

def updateDownstreamDependencies(stagedProject) {
  pushPomPropertyChangePR {
    propertyName = 'fabric8.version'
    projects = [
            'fabric8io/fabric8-devops',
            'fabric8io/fabric8-ipaas',
            'fabric8io/ipaas-quickstarts',
            'fabric8io/fabric8-forge',
            'fabric8io/kubeflix',
            'fabric8io/kubernetes-zipkin',
            'fabric8io/fabric8-maven-dependencies'
    ]
    version = stagedProject[1]
  }

  pushPomPropertyChangePR {
    parentPomLocation = 'parent/pom.xml'
    propertyName = 'fabric8.version'
    projects = [
            'fabric8io/funktion'
    ]
    version = stagedProject[1]
  }

  pushPomPropertyChangePR {
    parentPomLocation = 'parent/pom.xml'
    propertyName = 'version.fabric8'
    projects = [
            'fabric8io/fabric8-maven-plugin'
    ]
    version = stagedProject[1]
  }
}

return this;
