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

  class TopicDetails extends FON.TemplateController
    template: jade["activemq_detail_page/topic_details.jade"]
    template_data: -> @model.toJSON()

    initialize: ->
      super
      app.session_state.bind "change:selected_topic", =>
        @model = app.session_state.get "selected_topic"
        @model.bind "change", => @render()
        @render()

  
  class TopicRow extends FON.TemplateController
    tagName: "tr"
    template: jade["activemq_detail_page/topic_list_row.jade"]
    template_data: -> @model.toJSON()
    on_render: ->
      selected_topic = app.session_state.get "selected_topic"
      if selected_topic && selected_topic.id == @model.id
        $(@el).addClass("selected")
      else
        $(@el).removeClass("selected")
      
      $(@el).click (event) =>
        app.session_state.set
          selected_topic: @model
        false

  
  class TopicList extends FON.Table
    className: "zebra-striped"
    template: jade["activemq_detail_page/topic_list.jade"]

    initialize: ->
      super
      if !app.session_state.get "selected_topic" && @collection.models.length > 0
        app.session_state.set
          selected_topic: @collection.models[0]

      app.session_state.bind "change:selected_topic", => @render()

    child_control: (model) ->
      row = new TopicRow
        model: model
      model.bind "change", -> row.render()
      row


  class TopicDetailPage extends FON.ModelBackedTemplate
    template: jade["activemq_detail_page/topic_section.jade"]

    elements:
      ".topic-list": "topic_list_div"
      ".topic-overview": "topic_overview_div"

    initialize: ->
      super

      @topic_list = new TopicList
        collection: @model.topics()
      @topic_details = new TopicDetails
        model: app.session_state.get "selected_topic"
    
    on_render: ->
      @topic_list_div.html @topic_list.render().el
      @topic_overview_div.html @topic_details.render().el
    

  app.router.route "/containers/details/:name/broker/:broker/topics", "broker_details", (name, broker) ->
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

              app.page new TopicDetailPage
                model: model
                agent_name: name

          error: (model, resp, opts) ->
            app.router.navigate "/containers/details/#{name}/brokers", true
        else
          app.router.navigate "/agent/details/#{name}/brokers", true

      error: (model, resp, opts) ->
        app.router.navigate "/containers/details/#{name}/brokers", true

