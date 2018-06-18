#!/usr/bin/groovy
@Library('github.com/fabric8io/fabric8-pipeline-library@master')
def dummy
clientsTemplate{
  def javaOptions = '-Duser.home=/root/ -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dsun.zip.disableMemoryMapping=true -XX:+UseParallelGC -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -Xms10m -Xmx2048m'
  mavenNode (javaOptions:javaOptions) {
    ws{
      checkout scm
      sh "git remote set-url origin git@github.com:fabric8io/fabric8.git"

      def pipeline = load 'release.groovy'

      stage 'Stage'
      def stagedProject = pipeline.stage()

      stage 'Promote'
      pipeline.release(stagedProject)

      stage 'Update downstream dependencies'
      pipeline.updateDownstreamDependencies(stagedProject)
    }
  }
}
