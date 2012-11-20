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
  "models/versions"
  "models/upgrades"
  "controllers/controls/loading_page"
], (app, jade, Versions, Upgrades) ->

  class UpgradePage extends FON.TemplateController
    template: jade["upgrades_page/index.jade"]
    template_data: -> 
      {
        upgrades: @model.toJSON()
        versions: @versions.toJSON()
        upgrades_available: @upgrades_available
      }

    elements:
      ".opts": "opts"
      "select[name=version-select]": "version_select"

    events: ->
      "click a.select-all.btn": "select_all"
      "click a.select-none.btn": "select_none"
      "click a.apply.btn": "do_apply"
      "click a.refresh.btn": "do_refresh"

    initialize: ->
      super
      @versions = @options.versions
      @version = @options.version
      @model.bind "all", @render, @

      @upgrades_available = false
      for artifact, value of @model.attributes
        if value.length > 0
          @upgrades_available = true

    do_refresh: ->
      app.page new FON.LoadingPage
      @model.fetch
        success: (model, resp) =>
          app.page new UpgradePage
            model: model
            version: @version
            versions: @versions
        error: (model, resp, opts) ->
          app.flash
            kind: "error"
            title: "Error: "
            message: "Failed to retrieve upgrade data from server due to #{resp.statusText} : #{resp.responseText}"
            on_close: -> window.location.reload()              
      false

    do_apply: ->
      artifacts = $("input:checkbox:checked").map(-> @name).get()

      upgrades = {}

      for artifact in artifacts
        selector = "input[type=radio][name='#{artifact}']:checked"
        upgrades[artifact] = $(selector).val()

      data = {
        target_version: @version_select.val()
        upgrades: upgrades
      }

      options =
        success: (data, textStatus, jqXHR) =>
          app.flash
            kind: "info"
            title: "Upgrade successfully applied to version #{data}: "
            message: "To apply the updates to running containers use the \"Migrate Containers\" button to move containers to the new version #{data}"
            on_close: -> app.router.navigate "/containers", true
        error: (model, response, options) ->
          app.flash
            kind: "error"
            title: "Error Upgrading: "
            message: "Failed to apply updates due to #{response.statusText} : #{response.responseText}"
            on_close: -> window.location.reload()

      app.page new FON.LoadingPage
      @model.url = "rest/upgrades"
      @model.apply_upgrades data, options
      false

    select_all: -> 
      $("input:checkbox").attr("checked", true)
      false

    select_none: -> 
      $("input:checkbox").attr("checked", false)
      false

    on_render: ->
      @opts.find("input:radio:first").attr "checked", true
      @version_select.val @version

      @version_select.change (event) =>
        @version = $(event.currentTarget).val()
        model = new Upgrades
        model.url = "rest/upgrades/versions/#{@version}"
        app.page new FON.LoadingPage
        model.fetch
          success: (model, resp) =>
            app.page new UpgradePage
              model: model
              version: @version
              versions: @versions
          error: (model, resp, opts) ->
            app.flash
              kind: "error"
              title: "Error: "
              message: "Failed to retrieve upgrade data from server due to #{resp.statusText} : #{resp.responseText}"
              on_close: -> window.location.reload()              

    poll: ->
      @versions.fetch
        op: "update"



  app.router.route "/upgrades", "upgrades", ->
    app.page new FON.LoadingPage

    versions = new Versions

    versions.fetch
      success: (m, r) ->
        default_version = versions.default_version().id
        model = new Upgrades
        model.url = "rest/upgrades/versions/#{default_version}"
        model.fetch
          success: (model, resp) ->
            app.page new UpgradePage
              model: model
              versions: versions
              version: default_version
          error: (model, resp, opts) ->
            app.flash
              kind: "error"
              title: "Error: "
              message: "Failed to retrieve upgrade data from server due to #{resp.statusText} : #{resp.responseText}"
              on_close: -> window.location.reload()              
      error: ->
        app.flash
          kind: "error"
          title: "Error: "
          message: "Failed to retrieve data from server"
          on_close: -> window.location.reload()



