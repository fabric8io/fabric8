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
  "models/agents"
  "controllers/profile_list_control"
  "controllers/osgi_page"
  "controllers/jvm_metrics"
  "controllers/add_profile_dialog"
  "controllers/controls/loading_page"
], (app, jade, Agent, Agents, ProfileList, OSGiPage, JVMMetrics) ->


  class JvmDetails extends FON.TemplateController
    template: jade["agent_detail_page/jvm_overview.jade"]
    template_data: -> @model.toJSON()
    elements:
      ".metrics": "metrics"

    initialize: ->
      super
      @jvm_metrics = new JVMMetrics
        model: @model
      @model.bind "change", => @render()

    on_render: ->
      @metrics.html @jvm_metrics.render().el

    poll: ->
      @jvm_metrics.poll()


  class AgentDetails extends FON.ModelBackedTemplate
    template: jade["agent_detail_page/index.jade"]
    template_data: -> @model.toJSON()

    elements:
      ".jvm-overview" : "jvm_overview"
      ".buttons" : "buttons"
      ".provision-exception": "exception"
      ".osgi": "osgi"
      ".camel": "camel"
      ".mq": "mq"
      ".edit-list": "list"
      "a.add-profile": "add_profile"
    
    initialize: ->
      super
      @jvm_model = @options.jvm_model if @options.jvm_model

      #@jvm_model.bind "change", => @render()
      @model.bind "all", => 
        @configure_buttons()
        if @model.get "provision_exception"
          @exception.append "<strong>Provision Exception : </strong>"
          lines = @model.get("provision_exception").split "\n"
          for line in lines
            @exception.append """<p style="padding: 0px; margin: 0px;">#{line.replace(" ", "&nbsp;")}</p>"""
        else
          @exception.html("")

    maybe_enable: (button, name, collection, url) ->
      if _.contains(collection, name)
        button.removeClass "hide"
        button.click (event) =>
          app.router.navigate "/containers/details/#{@model.id}/#{url}", true
          false
      else
        button.addClass "hide"
        button.click (event) -> false

    configure_buttons: ->
      extensions = @model.get "extensions"

      @maybe_enable @osgi, "osgi", extensions, "osgi/bundles"
      @maybe_enable @camel, "camel", extensions, "camel_contexts"
      @maybe_enable @mq, "activemq", extensions, "brokers"

    on_render: ->
      ul = new ProfileList
        collection: @model.profiles()
      @list.html ul.render().el

      @add_profile.click (event) =>
        @model.fetch
          op: "update"
          success: =>
            app.versions.fetch
              op: "update"
              success: =>
                dialog = new FON.AddProfileDialog
                  model: @model
                dialog.render()
        false

      @jvm_details = new JvmDetails
        el: @jvm_overview
        model: @jvm_model

      @jvm_details.render()

      @configure_buttons()
                                       
    poll: ->
      @model.fetch
        op: "update"
      @jvm_details.poll()

  handle_error = (msg) ->
    app.flash
      kind: "error"
      title: "Error"
      message: "#{msg}"
      on_close: -> 
        app.router.navigate "/containers", true
        window.location.reload()


  app.router.route "/containers/details/:agent", "agent_details", (name) ->

    app.page new FON.LoadingPage

    model = new Agent
      id: name

    model.fetch
      success: (model, resp) ->
        agent = model        
        json_model = agent.toJSON()

        if _.contains(json_model.extensions, "jvm")
          url = "#{agent.url()}/extensions/jvm/metrics"
          model = FON.Model.singleton
            url: url

          model.fetch
            success: (model, resp) ->
              app.page new AgentDetails
                model: agent
                jvm_model: model

            error: (model, resp, opts) ->
              handle_error("Cannot access JVM details for container \"#{name}\"")
        else
          handle_error("Cannot access JVM details for container \"#{name}\"")

      error: (model, resp, opts) ->
        handle_error("Cannot access JVM details for container \"#{name}\"")

  AgentDetails
