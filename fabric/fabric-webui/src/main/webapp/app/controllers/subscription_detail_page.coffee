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
  "controllers/controls/label"
  "controllers/controls/tabs"
], (app, jade, ActiveMQs, Agents) ->


  class SubscriptionDetails extends FON.TemplateController
    template: jade["activemq_detail_page/subscription_details.jade"]
    template_data: -> @model.toJSON()

    initialize: ->
      super
      @model.bind "change", => @render()
      app.session_state.bind "change:selected_subscription", =>
        @model = app.session_state.get "selected_subscription"
        @model.bind "change", => @render()
        @render()

  
  class SubscriptionRow extends FON.TemplateController
    tagName: "tr"
    template: jade["activemq_detail_page/subscription_list_row.jade"]
    template_data: -> @model.toJSON()
    on_render: ->
      selected_subscription = app.session_state.get "selected_subscription"
      if selected_subscription && selected_subscription.id == @model.id
        $(@el).addClass("selected")
      else
        $(@el).removeClass("selected")
      
      $(@el).click (event) =>
        app.session_state.set
          selected_subscription: @model
        false

  
  class SubscriptionList extends FON.Table
    className: "zebra-striped"
    template: jade["activemq_detail_page/subscription_list.jade"]

    initialize: ->
      super
      if !app.session_state.get "selected_subscription" && @collection.models.length > 0
        app.session_state.set
          selected_subscription: @collection.models[0]

      app.session_state.bind "change:selected_subscription", => @render()

    child_control: (model) ->
      row = new SubscriptionRow
        model: model
      model.bind "change", -> row.render()
      row


  class SubscriptionDetailPage extends FON.ModelBackedTemplate
    template: jade["activemq_detail_page/subscription_section.jade"]

    elements:
      ".subscription-list": "subscription_list_div"
      ".subscription-overview": "subscription_overview_div"

    initialize: ->
      super

      if @options.subscription_type == "inactive"
        @subscriptions = @model.inactive_durable_topic_subscribers()
      else
        @subscriptions = @model.durable_topic_subscribers()

      @subscription_list = new SubscriptionList
        collection: @subscriptions
      @subscription_details = new SubscriptionDetails
        model: app.session_state.get "selected_subscription"
    
    on_render: ->
      @subscription_list_div.html @subscription_list.render().el
      @subscription_overview_div.html @subscription_details.render().el
    

  app.router.route "/containers/details/:name/broker/:broker/subscriptions/:type", "broker_details", (name, broker, subscription_type) ->
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

              if !app.session_state.get "selected_subscription_tab"
                app.session_state.set
                  selected_subscription_tab: "inactive"

              model.set
                agent_name: name
                type: subscription_type

              app.page new SubscriptionDetailPage
                model: model
                agent_name: name
                subscription_type: subscription_type

          error: (model, resp, opts) ->
            app.router.navigate "/containers/details/#{name}/brokers", true
        else
          app.router.navigate "/agent/details/#{name}/brokers", true

      error: (model, resp, opts) ->
        app.router.navigate "/containers/details/#{name}/brokers", true

