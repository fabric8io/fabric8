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
  "controllers/controls/table"  
], (app, jade) ->

  class VersionRow extends FON.TemplateController
    tagName: "tr"
    template: jade["agents_page/migrate_container_page/version_row.jade"]
    template_data: -> @model.toJSON()

    initialize: ->
      super
      @state = @options.state
      @state.bind "change:version", => @set_selected()

    set_selected: ->
      selected = @state.get "version"
      if selected && selected.id == @model.id
        @$el.addClass("selected")
      else
        @$el.removeClass("selected")

    on_render: ->      
      @$el = $(@el)
      @set_selected()

      @$el.click (event) =>
        @state.set
          version: @model
        false


  class VersionTable extends FON.Table
    className: "zebra-striped agents-list nav-list"
    template: -> jade["agents_page/migrate_container_page/version_list.jade"]
    
    child_control: (model) ->
      controller = new VersionRow
        state: @options.state
        model: model
      
      model.bind "change", -> controller.render()
      controller


  class MigrateAgentRow extends FON.TemplateController
    tagName: "tr"
    template: jade["agents_page/agent_row.jade"]
    template_data: -> @model.toJSON()
    
    initialize: ->
      super
      @state = @options.state

      @state.bind "change:#{@model.id}", => @set_selected()

    set_selected: ->
      val = @state.get "#{@model.id}"
      if val
        @$el.addClass("selected")
      else
        @$el.removeClass("selected")
    
    on_render: ->
      @$el = $(@el)
      @set_selected()

      @$el.click (event) =>
        val = @state.get "#{@model.id}"
        tmp = []
        tmp["#{@model.id}"] = !val
        @state.set tmp
        false


  class MigrateAgentTable extends FON.Table
    className: "zebra-striped agents-list nav-list"
    template: -> jade["agents_page/agent_list.jade"]

    child_control: (model)->
      controller = new MigrateAgentRow
        state: @options.state
        model: model

      model.bind "change", -> controller.render()
      controller


  class MigrateContainerPage extends FON.TemplateController
    template: jade["agents_page/migrate_container_page/index.jade"]

    elements:
      ".agent-table": "agent_table"
      ".version-select": "version_table"
      "a.apply": "apply"

    events:
      "click a.apply": "do_apply"
      "click a.cancel": "do_cancel"
      "click a.select-all": "do_select_all"
      "click a.select-none": "do_select_none"

    initialize: ->
      super
      @model.bind "change", => @render()

      @state = new FON.Model

      @state.bind "change", =>        
        state_json = @get_selected()
        container_selected = false

        for id,selected of state_json
          if selected
            container_selected = true

        if @apply
          if @state.has("version") && container_selected
            @apply.removeClass("disabled")
          else
            @apply.addClass("disabled")


      @set_all(false)

    get_selected: ->
      state_json = @state.toJSON()
      delete state_json.version if state_json.version
      state_json

    set_all: (state) ->
      for container in @model.models
        foo = []
        foo[container.id] = state
        @state.set foo
    
    do_apply: ->
      containers = []
      version = @state.get("version").id
      state_json = @get_selected()

      for id,selected of state_json
        if selected
          containers.push("#{id}")

      @model.migrate_containers
        containers: containers
        version: version
        success: (model, resp) =>
          @do_cancel()
        error: (model, data, resp) =>
          app.flash
            kind: "error"
            title: "Server Error: "
            message: "Failed to invoke on server: #{data}"
          @do_cancel()

      false

    do_select_all: ->
      @set_all(true)
      false

    do_select_none: ->
      @set_all(false)
      false

    do_cancel: ->
      @options.on_cancel() if @options.on_cancel
      false

    on_render: ->
      containers = new MigrateAgentTable
        state: @state
        collection: @model
      @agent_table.html containers.render().el

      versions = new VersionTable
        state: @state
        collection: app.versions
      @version_table.html versions.render().el

    poll: ->
      @model.fetch
        op: "update"
      app.versions.fetch
        op: "update"


