#!/usr/bin/groovy
node{
  ws{
    checkout scm
    sh "git remote set-url origin git@github.com:fabric8io/fabric8.git"

    def pipeline = load 'release.groovy'

    stage 'Updating dependencies'
    def prId = pipeline.updateDependencies('http://central.maven.org/maven2/')

    stage 'Stage'
    def stagedProject = pipeline.stage()

    stage 'Promote'
    pipeline.release(stagedProject)
    if (prId != null){
      pipeline.mergePullRequest(prId)
    }    
  }
}
