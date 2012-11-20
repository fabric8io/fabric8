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
  "views/jade"
], (app, jade) ->


  class RegistryPage extends FON.TemplateController
    template: jade["registry_page/index.jade"]
    template_data: -> 
      rc = @model.toJSON()

      path = @model.get("path")
      paths = []
      for each in path.split("/").reverse()
        if each != ""
          paths.push
            name: each
            path: path

        path = path.substring(0, path.lastIndexOf("/#{each}"))

      @model.set {paths: paths.reverse()}, {silent: true}
      @model.toJSON()

    elements:
      ".value": "value"
      "ul.children": "children"

    initialize: ->
      super
      @model.bind "change", @render, @

    on_render: ->

      if @model.get("value")          
        value = @model.get("value")
        @value.html """<textarea class="value-display" readonly="true">#{value}</textarea>"""

      path = @model.get("path")
      if path == "/"
        path = ""
      for child in @model.get("children")
        @children.append("<li><a href=\"#/registry#{path}/#{child}\">#{child}</a></li>")

    poll: ->
      @model.fetch
        op: "update"

  app.router.route "/registry", "registry", ->
    app.router.navigate "/registry/"

  app.router.route "/registry*path", "registry", (path) ->
    if !path
      path=""
    if path == "/"
      path=""
    model = new FON.model
    model.url = "rest/zookeeper#{path}"
    model.fetch
      success: (model, resp) ->
        app.page new RegistryPage
          model: model
