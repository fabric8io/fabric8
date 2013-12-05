###
 Copyright 2010 Red Hat, Inc.

 Red Hat licenses this file to you under the Apache License, version
 2.0 (the "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 implied.  See the License for the specific language governing
 permissions and limitations under the License.
###

define [
  "models/entry"
], (Entry, Agent) ->

  class Profile extends FON.Model
    id: null
    version: null
    _abstract: false
    children: []

    agents: FON.nested_collection("agents")
    bundles: FON.nested_collection("bundles", Entry)
    features: FON.nested_collection("features", Entry)
    fabs: FON.nested_collection("fabs", Entry)
    repositories: FON.nested_collection("repositories", Entry)
    config_props: FON.nested_collection("config_props", Entry)
    system_props: FON.nested_collection("system_props", Entry)
    configurations: FON.nested_collection("configurations", Entry)

    set_parents: (options) ->
      options = _.extend({
        url: "#{@url}/parents"
        type: "POST"
        data: JSON.stringify(options)
        contentType: "application/json"
        dataType: "json"
      }, options)
      $.ajax(options)

    set_attribute: (options) ->
      options = _.extend({
      url: "#{@url}/set_attribute"
      type: "POST"
      data: JSON.stringify(options)
      contentType: "application/json"
      dataType: "json"
      }, options)
      $.ajax(options)


  Profile
