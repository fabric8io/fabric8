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
  "models/agent"
  "models/camel"
  "models/camels"
  "controllers/controls/table"
  "controllers/controls/swappable_model_view"
], (app, jade, Agent, Camel, Camels) ->

  class RouteInfo extends FON.TemplateController
    template: jade["camel_page/route_info.jade"]
    template_data: ->
      parse_id @model.id, @model.toJSON()

    initialize: ->
      super
      @model.bind "change", @render, @


  class ContextOverview extends FON.SwappableModelView
    template: jade["camel_page/overview.jade"]
    template_data: -> @model.toJSON()

    elements:
      ".routes": "routes"

    do_render: -> @render()

    on_render: ->
      collection = new FON.CollectionController
        collection: @model.routes()
        child_control: (model) ->
          row = new RouteInfo
            model: model
          row

      @routes.html collection.render().el


  class ContextRow extends FON.TemplateController
    tagName: "tr"
    template: jade["camel_page/row.jade"]
    template_data: -> @model.toJSON()

    initialize: ->
      super
      @model.bind "change", @render, @

    on_render: ->
      selected = @options.parent.selected()
      el = $(@el)
      el.toggleClass "selected", (selected && selected.id == @model.id)

      el.click (event) =>
        @options.parent.selected @model
        false

  class ContextTable extends FON.Table
    className: "zebra-striped nav-list"
    template: jade["camel_page/table_header.jade"]

    child_control: (model) ->
      row = new ContextRow
        model: model
        parent: @options.parent
      row


  class CamelDetailPage extends FON.TemplateController
    template: jade["camel_page/index.jade"]

    state: new FON.Model

    template_data: ->
      agent_name: @agent_name

    elements:
      ".context-list": "context_list"
      ".context-overview": "context_overview"

    initialize: ->
      super

      @selected = @state.property("selected")
      @agent_name = @options.agent_name

      @old_selection = @model.at(0)
      if app.session_state.get "selected_context"
        context = @model.get(app.session_state.get("selected_context"))
        @old_selection = @model.get(context.id) if context

      app.session_state.set
        selected_context: @old_selection.id

      @selected @model.get(@old_selection.id)

      @context_table = new ContextTable
        collection: @model
        parent: @

      @overview = new ContextOverview
        model: @selected()
            
      @state.bind "change:selected", @selection_changed, @

    selection_changed: (one, two, three) ->
      old_selection = @old_selection
      new_selection = @selected()

      @overview.set_model new_selection
      old_selection.trigger("change") if old_selection
      new_selection.trigger("change") if new_selection      

      app.session_state.set
        selected_context: new_selection.id

      @old_selection = new_selection

    on_render: ->
      @context_list.html @context_table.render().el
      @context_overview.html @overview.render().el
      
    poll: ->
      @model.fetch
        op: "update"      


  handle_error = (msg, name) ->
    app.flash
      kind: "error"
      title: "Error"
      message: "#{msg}"
      on_close: -> app.router.navigate "/containers/details/#{name}", true

  app.router.route "/containers/details/:name/camel_contexts", "camel_context", (name) ->
    model = new Agent
      id: name

    model.fetch
      success: (model, resp) ->        
        agent = model
        if !agent
          app.router.navigate "/containers", true

        json_model = agent.toJSON()

        if _.contains(json_model.extensions, "camel")
          url = "#{agent.url()}/extensions/camel"
          camel_contexts = Camels.singleton
            url: url

          camel_contexts.fetch
            success: (model, resp) ->
              if model.length == 0
                handle_error("No Camel contexts are running in container \"#{name}\"", name)
              else
                app.page new CamelDetailPage
                  model: model
                  agent_name: name
            error: (model, resp, opts) ->
              handle_error("Cannot access Camel context details for agent \"#{name}\"", name)
        else
          handle_error("Cannot access Camel context details for agent \"#{name}\"", name)
      error: (model, resp, opts) ->
        handle_error("Cannot access Camel context details for agent \"#{name}\"", name)

  CamelDetailPage

