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
  "models/activemqs"
  "models/agent"
  "controllers/controls/table"
], (app, jade, ActiveMQs, Agent) ->


  class SimpleList extends FON.TemplateController

    initialize: ->
      @tagName = @options.tagName if @options.tagName
      @className = @options.className if @options.className
      super
      @child_control = @options.child_control if @options.child_control

    on_render: ->
      $(@el).empty()
      for item in @model
        controller = @child_control(item)
        $(@el).append(controller.render().el)


  class BasicBrokerDetails extends FON.ModelBackedTemplate
    template: jade["activemq_detail_page/basic_info.jade"]


  class QueueSection extends FON.TemplateController
    template: jade["activemq_detail_page/queue_section.jade"]

    elements:
      ".queue-list": "queue_list_div"
      ".queue-overview": "queue_overview_div"

    initialize: ->
      super

      @queue_list = new QueueList
        collection: @model.queues()
      @queue_details = new QueueDetails
        model: app.session_state.get "selected_queue"
    
    on_render: ->
      @queue_list_div.html @queue_list.render().el
      @queue_overview_div.html @queue_details.render().el


  class QueueDetails extends FON.TemplateController
    template: jade["activemq_detail_page/queue_details.jade"]
    template_data: -> @model.toJSON()

    initialize: ->
      super
      app.session_state.bind "change:selected_queue", =>
        @model = app.session_state.get "selected_queue"
        @model.bind "change", => @render()
        @render()

  
  class QueueRow extends FON.TemplateController
    tagName: "tr"
    template: jade["activemq_detail_page/queue_list_row.jade"]
    template_data: -> @model.toJSON()
    on_render: ->
      selected_queue = app.session_state.get "selected_queue"
      if selected_queue && selected_queue.id == @model.id
        $(@el).addClass("selected")
      else
        $(@el).removeClass("selected")
      
      $(@el).click (event) =>
        app.session_state.set
          selected_queue: @model
        false

  
  class QueueList extends FON.Table
    className: "zebra-striped"
    template: jade["activemq_detail_page/queue_list.jade"]

    initialize: ->
      super
      if !app.session_state.get "selected_queue" && @collection.models.length > 0
        app.session_state.set
          selected_queue: @collection.models[0]

      app.session_state.bind "change:selected_queue", => @render()

    child_control: (model) ->
      new QueueRow
        model: model


  class ConnectionSection extends FON.TemplateController
    className: "row connection-section"
    template: jade["activemq_detail_page/connection_section.jade"]
    elements:
      ".connectors": "connectors_div"
      ".connections": "connections_div"

    initialize: ->
      super

      @connectors = new FON.CollectionController
        tagName: "ul"
        className: "simple fixed250px"
        collection: @model.connectors()
        child_control: (connector) ->
          new ConnectorRow
            model: connector

      @connections = new FON.CollectionController
        tagName: "ul"
        className: "simple fixed250px" 
        collection: @model.connections()
        child_control: (connection) ->
          new ConnectionRow
            model: connection

    on_render: ->
      @connectors_div.html @connectors.render().el      
      @connections_div.html @connections.render().el


  class ConnectorRow extends FON.TemplateController
    tagName: "li"
    template: jade["activemq_detail_page/connector_info.jade"]
    template_data: ->
      name = @model.id.split(",")[1].split("=")[1]
      json_model = @model.toJSON()
      json_model["name"] = name
      json_model

  
  class ConnectionRow extends FON.TemplateController
    tagName: "li"
    template: jade["activemq_detail_page/connection_info.jade"]
    template_data: ->

      json_model = @model.toJSON()

      json_model["ViewType"] = "unknown"

      ids = @model.id.split(",")

      for id in ids
        name_value = id.split("=")
        json_model[name_value[0]] = name_value[1]

      json_model


  class ActiveMQDetailPage extends FON.TemplateController
    template: jade["activemq_detail_page/index.jade"]
    template_data: -> @model.toJSON()

    elements:
      ".basic-info": "basic_info"
      ".connection-info": "connection_info_div"
      ".queue-info": "queue_info_div"

    initialize: ->
      super
      @agent_name = @options.agent_name if @options.agent_name
      @broker_name = @model.get "name"

      @details = new BasicBrokerDetails
        model: @model

      @connection_info = new ConnectionSection
        model: @model
      
      @queue_info = new QueueSection
        model: @model
      
    on_render: ->
      @basic_info.html @details.render().el

      connection_info = new FON.Accordion
        className: "span16 columns"
        title: "Connection Details"
        open: app.session_state.property "connection_info"
        content: @connection_info

      @connection_info_div.html connection_info.render().el

      queue_info = new FON.Accordion
        className: "span16 columns"
        title: "Queue Details"
        open: app.session_state.property "queue_info"
        content: @queue_info
      
      @queue_info_div.html queue_info.render().el
      
    poll: ->
      @model.fetch
        op: "update"
            

  app.router.route "/containers/details/:name/broker/:broker", "broker_details", (name, broker) ->
    model = new Agent
      id: name

    model.fetch
      success: (model, resp) ->
        agent = model
        if !agent
          app.router.navigate "/containers", true

        json_model = agent.toJSON()

        if _.contains(json_model.extensions, "activemq")
          url = "#{agent.url()}/extensions/activemq"
          brokers = ActiveMQs.singleton
            url: url
        
          brokers.fetch
            success: (model, resp) ->
              model = (model for model in model.models when model.get("name") == broker)[0]

              if !model
                app.router.navigate "/containers/details/#{name}/brokers", true

              app.page new ActiveMQDetailPage
                model: model
                agent_name: name

          error: (model, resp, opts) ->
            app.router.navigate "/containers/details/#{name}/brokers", true
        else
          app.router.navigate "/agent/details/#{name}/brokers", true

      error: (model, resp, opts) ->
        app.router.navigate "/containers/details/#{name}/brokers", true

