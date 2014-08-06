#
#  Copyright 2005-2014 Red Hat, Inc.
#
#  Red Hat licenses this file to you under the Apache License, version
#  2.0 (the "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
#  implied.  See the License for the specific language governing
#  permissions and limitations under the License.
#

Note that the import directory just contains the initial import data used when creating a new fabric.

Once a fabric has been created this directory is completely ignored!

To import your custom profiles, you can drop in .zip files with the profiles.
For example use the fabric8:zip plugin to generate the .zip files.

Instead of dropping in the .zip files in this directory, you can create a .properties file,
and define the url's for the .zip files. For mvn coordinates, then they usually of the form mvn:groupId/artifactId/version/zip/profile

