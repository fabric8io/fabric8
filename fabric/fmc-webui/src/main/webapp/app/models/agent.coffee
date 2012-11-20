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
  "models/app"
], (app) ->

  class Agent extends FON.Model

    urlRoot: "rest/agents"
        
    profiles: FON.nested_collection("profiles")
    children: FON.nested_collection("children", Agent)

    parse: (response) ->
      if _.isArray(response)
        mask = false
        message = ""
        for container in response
          if container.failure
            mask = true
            if container.success == false
              message = message + "Failed to create container \"#{container.containerName}\", message: \"#{container.failure.localizedMessage}\"<br>\n"

        index = message.lastIndexOf("<br>\n")
        message = message.slice(0, index)

        if message.length > 0
          FON.app.flash
            kind: "error"
            title: "Container Creation Failure : "
            message: message
          []
        else
          if mask
            []
          else    
            response
      else 
        response

    start: (options)-> 
      options = _.extend({
        url: "#{@url()}/start"
        type: "POST"
      }, options) 
      $.ajax(options)
      
    stop: (options)-> 
      options = _.extend({
        url: "#{@url()}/stop"
        type: "POST"
      }, options) 
      $.ajax(options)

  Agent

