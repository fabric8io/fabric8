##-------------------------------------------------------------------------
## Copyright 2013 Red Hat, Inc.  All rights reserved.
##  http://www.redhat.com
##
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##    http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
##-------------------------------------------------------------------------


#
#	The location of the local repository that will be created and 
#   referenced for all future builds as long as it exists.
#	
#	Defaults to <fusecdc home>/repository
#	
#		mvn:<groupId>/<artifactId>/<version>/<type>
#
# local.repository=./repository

#
#	The base artifact to start with.  It is suggested that you start with the 
#	minimal distribution to ensure the smallest and cleanest custom distribution.
#	
#	The notation below is in the form of a maven coordinate string:
#	
#		mvn:<groupId>/<artifactId>/<version>/<type>
#
original.distribution.artifact=mvn:org.jboss.fuse/jboss-fuse-minimal/${fuse.version}/zip

#
#	This property is used to add any custom or 3rd party feature descriptor XML files.
#	The descriptors defined here will be added to those discovered in the distribution 
#	defined above in the distribution.artifact property to allow for discovery of 
#	transitive features. 
#
# feature.descriptors=

#
#	The features property is where you can list the feature names
#	to copy to the distributions system repository.
# 
#	This property affects the output in one of 3 ways:
#
#		empty/undefined <default>
#			Leaving the property commented out or empty will 
#			resolve all the features defined in the 
#			org.apache.karaf.features.cfg file.
#		
#		feature name list
#			A comma delimited list of feature names.  When using this 
#			property, the FuseCDC will always ensure that the following 
#			default minimums are also loaded into the repository:
#			
#				karaf-framework
#				config
#				fabric-boot-commands
#				fabric-bundle
#				patch
# 
# features.names=

#	
#	The directory to extract the defined distribution to.  Defaults to build.
#	
build.output.directory=build

#	
#	Whether or not to repackage the resultant output.  Defaults to true.
#	
distribution.create=true

#
#	The directory to create the new distribution in.  Defaults to dist.
#	
distribution.output.directory=dist

#	
#	The name of the new distribution file.  Defaults to the 
#	name of the original artifact root directory with a 
#	classifier of 'custom'.
#	 
distribution.output.final.name=fuse-esb-${fuse.version}-custom

#	Repository Proxy
#	
#   A string made up the the protocol, host & port for the proxy.
#   It can optionally contain the username and password if required.
#     http.proxy=(http|https)://[username:password]@host:port
#
#http.proxy=

#	
#	Repository Definitions
#
#   Repository ID: (REQUIRED)
#	  repository.<no> = 
#
#   Repository URL: (REQUIRED)
#	  repository.<no>.url = 
#
#   Repository User Name: 
#     repository.<no>.username = 
#
#   Releases Enabled: 
#     repository.<no>.password =
#
#   Repository Layout: 
#     repository.<no>.layout = default (default) or legacy
#
#   Releases Enabled: 
#     repository.<no>.snapshots.enabled = true (default) or false
#
#   Release Update Policy: 
#     repository.<no>.release.policy.update = daily (default), always, never
#
#   Release Checksum Policy: 
#     repository.<no>.release.policy.checksum = warn (default), ignore or fail
#
#   Snapshot Enabled: 
#     repository.<no>.snapshots.enabled = true (default) or false
#
#   Snapshot Update Policy: 
#     repository.<no>.snapshots.policy.update = daily (default), always, never
#
#   Snapshot Checksum Policy: 
#     repository.<no>.snapshot.policy.checksum = warn (default), ignore or fail
#
repository.1=repo.fusesource.com
repository.1.url=https://repo.fusesource.com/nexus/content/groups/public
repository.1.username=
repository.1.password=
repository.1.layout=default
repository.1.release.enabled=true

repository.2=snapshot.repo.fusesource.com
repository.2.url=https://repo.fusesource.com/nexus/content/groups/public-snapshots
repository.2.username=
repository.2.password=
repository.2.layout=default
repository.2.release.enabled=false
repository.2.snapshots.enabled=true

repository.3=ea.repo.fusesource.com
repository.3.url=https://repo.fusesource.com/nexus/content/groups/ea
repository.3.release.enabled=true
repository.3.snapshots.enabled=true

repository.4=svn.apache.org
repository.4.url=http://svn.apache.org/repos/asf/servicemix/m2-repo

repository.5=scala-tools.org
repository.5.url=https://oss.sonatype.org/content/groups/scala-tools

repository.6=releases.springsource.com
repository.6.url=http://repository.springsource.com/maven/bundles/release

repository.7=external.springsource.com
repository.7.url=http://repository.springsource.com/maven/bundles/external

repository.8=central
repository.8.url=http://repo1.maven.org/maven2


## ---------------------------------------------------------------------------
## Maven Wagon Read Time Out
## See http://maven.apache.org/wagon/wagon-providers/wagon-http/ for additional
## Maven Wagon HTTP configuation options.
##
## FuseCDC sets a default read timout of 10000 ms
## ---------------------------------------------------------------------------
maven.wagon.rto=10000
