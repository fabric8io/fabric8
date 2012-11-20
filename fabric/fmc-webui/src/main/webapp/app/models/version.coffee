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
  "models/profiles"
  "models/profile",
], (Profiles, Profile)->

  class Version extends FON.Model
    urlRoot: "rest/versions"
    id: null
    derived_from: null
    default: null
    profiles: FON.nested_collection("profiles", Profile)

    default_profile: ->
      rc = null
      for profile in @profiles().models
        if profile.id == "default"
          rc = profile
          break
      rc

    create_profile: (options) ->
      options = _.extend({
        url: "#{@url()}/profiles"
        type: "POST"
        data: JSON.stringify(options)
        contentType: "application/json"
        dataType: "json"
      }, options)
      $.ajax(options)

    delete_profiles: (options) ->
      options = _.extend({
        url: "#{@url()}/delete_profiles"
        type: "POST"
        data: JSON.stringify(options)
        contentType: "application/json"
        dataType: "json"
      }, options)
      $.ajax(options)


