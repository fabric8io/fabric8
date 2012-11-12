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
  "views/charts"
  "models/activemqs"
  "models/agent"
  "controllers/controls/swappable_model_view"  
], (app, jade, charts, ActiveMQs, Agent) ->

  class BrokerOverview extends FON.SwappableModelView
    template: jade["activemq_page/overview.jade"]
    template_data: -> @model.toJSON()
    do_render: -> @render()


  class BrokerRow extends FON.TemplateController
    tagName: "tr"
    template: jade["activemq_page/row.jade"]
    template_data: -> @model.toJSON()

    on_render: ->
      selected = @options.parent.selected()
      el = $(@el)
      el.toggleClass "selected", (selected && selected.id == @model.id)

      el.click (event) =>
        @options.parent.selected @model
        false


  class BrokerTable extends FON.Table
    className: "zebra-striped nav-list"
    template: jade["activemq_page/table_header.jade"]

    child_control: (model) ->
      row = new BrokerRow
        model: model
        parent: @options.parent
      model.bind "change", row.render, row
      row


  class BrokerDetailButtons extends FON.SwappableModelView
    template: jade["activemq_page/controls.jade"]
    elements:
      ".connections": "connections"
      ".network-connections": "ncs"
      ".queues": "queues"
      ".topics": "topics"
      ".active-subscriptions": "active_subscriptions"
      ".inactive-subscriptions": "inactive_subscriptions"

    do_render: -> @render()

    current_broker_name: -> @model.get "name"

    maybe_enable: (button, collection, url) ->
      if collection.length == 0
        button.addClass "hide"
      else
        button.removeClass "hide"

      if !button.hasClass "hide"
        button.click (event) =>
          app.router.navigate "/containers/details/#{@options.agent_name}/broker/#{@current_broker_name()}/#{url}", true
          false
      else
        button.click (event) -> false

    on_render: ->
      @maybe_enable(@connections, @model.connections().models, "connections")
      @maybe_enable(@queues, @model.queues().models, "queues")
      @maybe_enable(@topics, @model.topics().models, "topics")
      @maybe_enable(@ncs, @model.network_connectors().models, "ncs")
      @maybe_enable(@active_subscriptions, @model.durable_topic_subscribers().models, "subscriptions/active")
      @maybe_enable(@inactive_subscriptions, @model.inactive_durable_topic_subscribers().models, "subscriptions/inactive")
                                      

  class ActiveMQController extends FON.TemplateController
    template: jade["activemq_page/index.jade"]

    state: new FON.Model

    template_data: ->
      agent_name: @agent_name

    elements:
      ".broker-list": "broker_list"
      ".broker-overview": "broker_overview"
      ".controls": "controls_div"

    initialize: ->
      super

      @selected = @state.property("selected")
      @agent_name = @options.agent_name

      @old_selection = @model.at(0)
      if app.session_state.get "selected_broker"
        broker = @model.get(app.session_state.get("selected_broker"))
        @old_selection = @model.get(broker.id) if broker

      app.session_state.set
        selected_broker: @old_selection.id

      @selected @old_selection

      @overview = new BrokerOverview
        model: @selected()

      @controls = new BrokerDetailButtons
        model: @selected()
        agent_name: @agent_name

      @broker_table = new BrokerTable
        collection: @model
        parent: @

      @state.bind "change:selected", @selection_changed, @

    selection_changed: ->  
      old_selection = @old_selection
      new_selection = @selected()

      @controls.set_model new_selection
      @overview.set_model new_selection

      old_selection.trigger("change") if old_selection
      new_selection.trigger("change") if new_selection

      app.session_state.set
        selected_broker: new_selection.id

      @old_selection = new_selection

    on_render: ->
      @controls_div.html @controls.render().el
      @broker_list.html @broker_table.render().el
      @broker_overview.html @overview.render().el

    poll: ->
      @model.fetch
        op: "update"


  handle_error = (msg, name) ->
    app.flash
      kind: "error"
      title: "Error"
      message: "#{msg}"
      on_close: -> app.router.navigate "/containers/details/#{name}", true

  app.router.route "/containers/details/:name/brokers", "brokers", (name) ->
    model = new Agent
      id: name

    model.fetch
      success: (model, resp) ->        
        agent = model
        json_model = agent.toJSON()

        if _.contains(json_model.extensions, "activemq")
          url = "#{agent.url()}/extensions/activemq"
          brokers = ActiveMQs.singleton
            url: url

          brokers.fetch
            success: (model, resp) ->
              if model.length == 0
                handle_error("No brokers have been deployed in container \"#{name}\"", name)
              else
                app.page new ActiveMQController
                  model: model
                  agent_name: name
            error: (model, resp, opts) ->
              handle_error("Cannot access broker details for agent \"#{name}\"", name)
        else
          handle_error("Cannot access broker details for agent \"#{name}\"", name)
      error: (model, resp, opts) ->
        handle_error("Cannot access broker details for agent \"#{name}\"", name)
      
  ActiveMQController
