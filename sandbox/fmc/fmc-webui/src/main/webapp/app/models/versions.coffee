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
  "models/version",
], (Version)->

  class Versions extends FON.Collection
    model: Version
    url: "rest/versions"

    default_version: ->
      for version in @models
        if version.get "_default"
          return version
      null

    create_version: (options) ->
      options = _.extend({
        url: "#{@url}"
        type: "POST"
        data: JSON.stringify(options)
        contentType: "application/json"
        dataType: "json"
      }, options)
      $.ajax(options)

    delete_versions: (options) ->
      options = _.extend({
        url: "#{@url}/delete"
        type: "POST"
        data: JSON.stringify(options)
        contentType: "application/json"
        dataType: "json"
      }, options)
      $.ajax(options)

    change_default: (options) ->
      options = _.extend({
        url: "#{@url}/set_default"
        type: "POST"
        data: JSON.stringify(options)
        contentType: "application/json"
        dataType: "json"
      }, options)
      $.ajax(options)
      

    

