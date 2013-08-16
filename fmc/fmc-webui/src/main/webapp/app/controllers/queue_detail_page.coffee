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

  class QueueDetails extends FON.TemplateController
    template: jade["activemq_detail_page/queue_details.jade"]
    template_data: -> @model.toJSON()

    initialize: ->
      super
      app.session_state.bind "change:selected_queue", =>
        @model = app.session_state.get "selected_queue"
        @model.bind "change", => @render()
        @render()
    
    poll: ->
      @model.fetch
        op: update

  
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
    className: "zebra-striped big"
    template: jade["activemq_detail_page/queue_list.jade"]

    initialize: ->
      super
      if !app.session_state.get "selected_queue" && @collection.models.length > 0
        app.session_state.set
          selected_queue: @collection.models[0]

      app.session_state.bind "change:selected_queue", => @render()

    child_control: (model) ->
      row = new QueueRow
        model: model
      model.bind "change", -> row.render()
      row


  class QueueDetailPage extends FON.ModelBackedTemplate
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
    

  app.router.route "/containers/details/:name/broker/:broker/queues", "broker_details", (name, broker) ->
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

              app.page new QueueDetailPage
                model: model
                agent_name: name

          error: (model, resp, opts) ->
            app.router.navigate "/containers/details/#{name}/brokers", true
        else
          app.router.navigate "/agent/details/#{name}/brokers", true

      error: (model, resp, opts) ->
        app.router.navigate "/containers/details/#{name}/brokers", true

