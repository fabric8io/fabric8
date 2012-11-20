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
  "models/agents"
  "controllers/profile_list_control"
  "controllers/add_agent_wizard"
  "controllers/migrate_container_page"
  "controllers/add_profile_dialog"
  "controllers/controls/collection"
  "controllers/controls/composite"
  "controllers/controls/table"
  "controllers/controls/editable_property"
  "controllers/controls/swappable_model_view"
  "controllers/controls/loading_page"
], (app, jade, Agents, ProfileList, AddAgentWizard, MigrateContainerPage) ->


  class AgentOverview extends FON.SwappableModelView
    template: jade["agents_page/agent_overview.jade"]
    template_data: -> @model.toJSON()
    elements:
      ".edit-list": "list"
      "td.ep": "editable_properties"
      "td.resolver": "resolver"
      "a.exception": "exception"
      "a.ssh": "ssh"
      "a.jmx": "jmx"

    do_render: -> @render()

    on_render: ->

      @ssh.click =>
        app.flash
          kind: "info"
          title: "SSH URL: "
          message: @model.get "ssh_url"
        false

      @jmx.click =>
        app.flash
          kind: "info"
          title: "JMX URL: "
          message: @model.get "jmx_url"
        false



      @exception.click =>
        app.flash
          kind: "info"
          title: "Provision Exception Trace: "
          message: @model.get "provision_exception"
        false
      
      for cell in @editable_properties
        ep = new FON.EditableProperty
          el: cell
          property: @model.property(cell.className.split(" ")[1])
        ep.render()

      resolver_control = new FON.EditableSelect
        el: @resolver
        property: @model.property("resolver")
        opts:
          "localhostname": "Local Hostname"
          "localip": "Local IP"
          "publichostname": "Public Hostname"
          "publicip": "Public IP"
          "manualip": "Manual IP"

      resolver_control.render()
      
      ul = new ProfileList
        collection: @model.profiles()
      @list.html ul.render().el


  class AgentRow extends FON.TemplateController
    tagName: "tr"
    template: jade["agents_page/agent_row.jade"]
    template_data: -> @model.toJSON()
    interval_id: null

    elements:
      "span#provision": "provision"

    initialize: ->
      super
      @model.bind "change", @render, @

    remove: ->
      super
      @model.unbind()

    toggle_class: ->
      @provision.toggleClass("yellow-dot.png")
      @provision.toggleClass("pending.gif")

    on_render: ->

      if @model.get("provision_indicator") == "pending.gif" && @interval_id == null
        @interval_id = setInterval (=> @toggle_class()), 250
      else if @model.get("provision_indicator") != "pending.gif" && @interval_id != null
        clearInterval @interval_id
        @interval_id = null


      selected = @options.parent.selected()
      el = $(@el)
      el.toggleClass "selected", (selected && selected.id == @model.id)

      el.click (event) =>
        @options.parent.selected @model
        false


  class AgentTable extends FON.Table
    className: "zebra-striped agents-list nav-list"
    template: -> jade["agents_page/agent_list.jade"]

    remove: -> @collection.reset()

    child_control: (model)->
      controller = new AgentRow
        model: model
        parent: @options.parent
      controller


  class AgentControlButtons extends FON.SwappableModelView
    template: jade["agents_page/controls.jade"]
    elements:
      "a.delete": "delete"
      "a.control": "control"
      "a.profile": "add_profile"
      "a.details": "details_btn"

    events:
      "click a.delete": "do_delete"
      "click a.control": "do_control"
      "click a.profile": "do_add_profile"
      "click a.details": "goto_details"

    do_render: -> @render()

    disable_control: -> @control.addClass "disabled"

    enable_control: -> @control.removeClass "disabled"

    do_control: (event) ->
      if @control.hasClass "disabled"
        return false

      if !@model.get "alive"
        @control.text "Starting..."
        @model.start
          error: => app.handle_ajax_error
      else
        dialog = new FON.ConfirmationDialog
          backwards: true
          model: @model
          button: @control
          on_close: => @render()
          header: -> "Stop #{@model.id}?"
          body_text: -> "Are you sure you want to stop \u201c#{@model.id}\u201d?"
          on_accept: ->
            @options.button.addClass "disabled"
            @do_hide()
            @options.button.text "Stopping..."
            @model.stop
              error: (model, response, xhr) =>
                message = "Failed to stop \u201c#{@model.id}\u201d"
                if xhr.responseText && xhr.responseText != ""
                  message = "#{message} - #{xhr.responsetext}"
                app.flash
                  kind: "error"
                  title: "Error: "
                  message: message
                  on_close: => @options.on_close()
        dialog.render()
      false

    do_delete: (event) ->
      if @delete.hasClass "disabled"
        return false

      dialog = new FON.ConfirmationDialog
        backwards: true
        model: @model
        button: @delete
        on_close: => @render()
        header: -> "Delete #{@model.id}?"
        body_text: -> "Are you sure you want to delete \u201c#{@model.id}\u201d?"
        on_accept: ->
          @options.button.addClass "disabled"
          @do_hide()
          @options.button.text("Deleting...")
          @model.destroy
            success: (model, response) =>
              
            error: (model, response, xhr) =>
              message = "Failed to delete \u201c#{@model.id}\u201d"
              if xhr.responseText && xhr.responseText != ""
                message = "#{message} - #{xhr.responsetext}"
              app.flash
                kind: "error"
                title: "Error: "
                message: message
                on_close: => @options.on_close()

      dialog.render()
      false

    goto_details: (event) ->
      if !@details_btn.hasClass "disabled"
        app.router.navigate "/containers/details/#{@model.id}", true
      false

    do_add_profile: (event) ->
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

    on_render: ->
      @delegateEvents @events
      
      if !@model.get "metadata"
        @control.addClass "hide"
        @delete.addClass "hide"

      @enable_control()
      if @model.get "alive"
        @control.text "Stop"
        @details_btn.removeClass "disabled"
      else
        @control.text "Start"
        @details_btn.addClass "disabled"
        
      if @delete.hasClass "disabled"
        @delete.removeClass "disabled"
        @delete.text "Delete"


  class AgentsController extends FON.TemplateController
    template: jade["agents_page/index.jade"]
    elements:
      "#agent_nav_container": "agent_nav_container"
      "#agent_details_container": "details"
      ".controls": "controls_div"
      "a.add": "add"

    events:
      "click a.migrate": "do_migrate"
      "click a.add": "do_add"

    state: new FON.Model
      
    initialize: ->
      super

      @selected = @state.property("selected")

      @old_selection = @model.at(0)
      if app.session_state.get "selected_agent"
        agent = @model.get(app.session_state.get("selected_agent"))
        @old_selection = @model.get(agent.id) if agent

      app.session_state.set
        selected_agent: @old_selection.id

      @selected @model.get(@old_selection.id)

      @page = new AgentTable
        collection: @model
        parent: @
      
      @overview = new AgentOverview
        model: @selected()

      @controls = new AgentControlButtons
        model: @selected()

      @state.bind "change:selected", @selection_changed, @

      @model.bind "add", @container_added, @
      @model.bind "remove", @container_removed, @

    remove: ->
      super
      @controls.remove()
      @overview.remove()
      @page.remove()
      @state.unbind()
      @model.unbind()

    container_added: (container) ->
      @selected container
      @controls_div.html @controls.render().el

    container_removed: (options) ->
      if !@selected() || @selected().id == options.id
        @selected @model.at(0)

    selection_changed: ->
      old_selection =  @old_selection
      new_selection = @selected()

      @controls.set_model new_selection
      @overview.set_model new_selection
      old_selection.trigger("change") if old_selection        
      new_selection.trigger("change") if new_selection

      app.session_state.set
        selected_agent: new_selection.id

      @old_selection = new_selection
        
    do_migrate: ->
      app.page new MigrateContainerPage
        model: @model
        on_cancel: => 
          @reset()
      false

    do_add: (event) ->
      app.page new FON.LoadingPage
      app.versions.fetch
        success: =>
          app.page new AddAgentWizard
            model: @model
            do_return: =>
              @reset()
      false

    on_render: ->
      @controls_div.html @controls.render().el
      @details.html @overview.render().el
      @agent_nav_container.html @page.render().el

    poll: ->
      @model.fetch
        op:"update"

    reset: ->
      create_page()

  handle_error = (msg) ->
    app.flash
      kind: "error"
      title: "Error"
      message: "#{msg}"
      on_close: -> window.location.reload()

  create_page = ->
    app.page new FON.LoadingPage
    model = new Agents
    model.fetch
      success: (model, resp) =>

        app.page new AgentsController
          model: model

      error: (model, resp, opts) =>
        handle_error("Failed to access container details")

  app.router.route "/containers", "agents", create_page

  AgentsController
