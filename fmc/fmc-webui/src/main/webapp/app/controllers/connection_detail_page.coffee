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
  "models/agents"
  "controllers/controls/table"
], (app, jade, ActiveMQs, Agents) ->


  class ConnectorRow extends FON.TemplateController
    tagName: "tr"
    template: jade["activemq_detail_page/connector_info.jade"]
    template_data: ->
      name = @model.id.split(",")[1].split("=")[1]
      json_model = @model.toJSON()
      json_model["name"] = name
      json_model

    
  class ConnectorList extends FON.Table
    className: "zebra-striped"
    template: jade["activemq_detail_page/connector_list.jade"]

    child_control: (model) ->
      new ConnectorRow
        model: model


  class ConnectionDetails extends FON.TemplateController
    template: jade["activemq_detail_page/connection_details.jade"]
    template_data: ->
      json_model = @model.toJSON()
      json_model["ViewType"] = "unknown"
      parse_id @model.id, json_model

    on_render: ->
      app.session_state.bind "change:selected_connection", =>
        @model = app.session_state.get "selected_connection"
        @render()


  class ConnectionRow extends FON.TemplateController
    tagName: "tr"
    template: jade["activemq_detail_page/connection_row.jade"]
    template_data: ->
      json_model = @model.toJSON()
      json_model["ViewType"] = "unknown"
      parse_id @model.id, json_model
    
    on_render: ->
      selected_connection = app.session_state.get "selected_connection"
      if selected_connection && selected_connection.id == @model.id
        $(@el).addClass("selected")
      else
        $(@el).removeClass("selected")

      $(@el).click (event) =>
        app.session_state.set
          selected_connection: @model
        false
    

  class ConnectionList extends FON.Table
    className: "zebra-striped"
    template: jade["activemq_detail_page/connection_list.jade"]

    initialize: ->
      super
      if !app.session_state.get "selected_connection" && @collection.models.length > 0
        for model in @collection.models
          if model.id.indexOf("ViewType") != -1            
            app.session_state.set
              selected_connection: model
      app.session_state.bind "change:selected_connection", => @render()
    
    child_control: (model) ->
      row = new ConnectionRow
        model: model
      row


  class ConnectionDetailPage extends FON.ModelBackedTemplate
    template: jade["activemq_detail_page/connection_section.jade"]
    elements:
      ".connectors": "connectors_div"
      ".connections": "connections_div"
      ".overview": "overview"

    initialize: ->
      super

      app.session_state.unset "selected_connection", {silent: true}

      @connectors = new ConnectorList
        collection: @model.connectors()
      
      @connections = new ConnectionList
        collection: @model.connections()
      
      @details = new ConnectionDetails
        model: app.session_state.get "selected_connection"

      app.session_state.bind "change:selected_connection", =>
        @details.model = app.session_state.get "selected_connection"
        @overview.html @details.render().el if @details.model

      @model.connectors().fetch
        success: => @render()
      @model.connections().fetch
        success: => @render()

    on_render: ->
      @connectors_div.html @connectors.render().el if @connectors
      @connections_div.html @connections.render().el if @connectors
      @overview.html @details.render().el if @details.model
  

  app.router.route "/containers/details/:name/broker/:broker/connections", "broker_details", (name, broker) ->
    model = new Agents
    model.fetch
      success: (model, resp) ->
        agent = (model for model in model.models when model.id == name)[0]
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

              model.set
                agent_name: name

              app.page new ConnectionDetailPage
                model: model
                agent_name: name

          error: (model, resp, opts) ->
            app.router.navigate "/containers/details/#{name}/brokers", true
        else
          app.router.navigate "/agent/details/#{name}/brokers", true

      error: (model, resp, opts) ->
        app.router.navigate "/containers/details/#{name}/brokers", true

