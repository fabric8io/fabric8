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
  "controllers/modal_wait_dialog"
  "controllers/controls/validating_text_input"
], (app, jade, ModalWaitDialog) ->


  class BasePanel extends FON.TemplateController
    initialize: ->
      super
      @parent = @options.parent if @options.parent

    back: ->
      @parent.panel.set
        active: new ButtonPanel
          model: @model
          parent: @parent
      false

  class CreatePanel extends BasePanel
    template: jade["welcome_page/create_panel.jade"]

    elements:
      "#manualip-val": "manualip_div"
      "select[name=local-resolver]": "local_resolver"
      "select[name=global-resolver]": "global_resolver"
      "input[name=manualip-val]": "manualip"
      "a.create": "create"
      "input[name=new-username]": "username"
      ":input[name=password]": "password"
      ":input[name=role]": "role"
      ":input[name=zk-password]": "zk_password"
      ":input[name=max-port]": "max_port"
      ":input[name=min-port]": "min_port"

    events:
      "click a.back": "back"
      "click a.create": "create_ensemble"

    create_ensemble: ->
      if !@create.hasClass("disabled")
        if @model.get "zk_cluster_service_available"
          @parent.wait.render()
          if @model.get("managed")
            @old_ajax_handler = app.handle_ajax_error
            app.handle_ajax_error = (resp, next) ->

          @model.create_ensemble
            username: @username.val()
            password: @password.val()
            role: @role.val()
            zk_password: @zk_password.val()
            global_resolver: @global_resolver.val()
            local_resolver: @local_resolver.val()
            manualip: @manualip.val()
            max_port: @max_port.val()
            min_port: @min_port.val()

            success: =>
              if @model.get("managed")
                @model.bind "change:provision_complete", =>
                  if @model.get("provision_complete")
                    app.handle_ajax_error = @old_ajax_handler
                    @parent.hide_wait()
                , @
              else
                setTimeout ( => @parent.hide_wait()), 2000
            error: (data) =>
              if @model.get("managed")
                app.handle_ajax_error = @old_ajax_handler
              @parent.hide_wait()
              app.flash
                kind: "error"
                title: "Error creating ensemble server"
                message: "Unable to create ensemble server, error message was #{data.responseText}"
                on_close: -> window.location.reload()
      false

    maybe_enable_create: (self) ->
      valid = true

      for control in self.validated
        if !control.validate() && valid
          valid = false

      if valid
        self.create.removeClass("disabled")
      else
        self.create.addClass("disabled")


    on_render: ->
      @validated = []

      @validated.push new FON.ValidatingTextInput
        control: @username
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false
            msg: "Must specify a username"
          else
            ok: true
            msg: ""
        cb: @maybe_enable_create

      @validated.push new FON.ValidatingTextInput
        control: @password
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false
            msg: "Must specify a password"
          else
            ok: true
            msg: ""
        cb: @maybe_enable_create

      @validated.push new FON.ValidatingTextInput
        control: @role
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false
            msg: "Must specify a role"
          else
            ok: true
            msg: ""
        cb: @maybe_enable_create

      @validated.push new FON.ValidatingTextInput
        control: @max_port
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false,
            msg: "You must specify a valid port number"
          else if isNaN(text)
            ok: false,
            msg: "You must specify a valid port number"
          else
            ok: true,
            msg: ""
        cb: @maybe_enable_create

      @validated.push new FON.ValidatingTextInput
        control: @min_port
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false,
            msg: "You must specify a valid port number"
          else if isNaN(text)
            ok: false,
            msg: "You must specify a valid port number"
          else
            ok: true,
            msg: ""


      $(@el).bind "DOMNodeInsertedIntoDocument", =>
        @maybe_enable_create(@)

      @local_resolver.change (event) =>
        @manualip_div.toggleClass "hide", @local_resolver.val() != "manualip"



  class JoinPanel extends BasePanel
    template: jade["welcome_page/join_panel.jade"]

    elements:
      "input[name='host']": "host"
      "input[name='port']": "port"
      "input[name='password']": "password"
      "a.join": "join"

    events:
      "click a.back": "back"
      "click a.join": "join_ensemble"

    join_ensemble: ->
      @parent.wait.render()
      @model.bind "change:client_connected", -> window.location.reload()

      @model.join_ensemble
        zk_url: "#{@host.val()}:#{@port.val()}"
        password: @password.val()
        success: =>
            setTimeout ( => @parent.hide_wait()), 2000
        error: (data) =>
          @parent.wait.do_hide()

          app.flash
            kind: "error"
            title: "Error creating ensemble server"
            message: "Unable to join existing ensemble, did you specify the correct hostname and port for the registry server?"
            on_close: -> window.location.reload()
      false

    maybe_enable_join: (self) ->
      valid = true

      for control in self.validated
        if !control.validate() && valid
          valid = false

      if valid
        self.join.removeClass("disabled")
      else
        self.join.addClass("disabled")

    on_render: ->

      @validated = []

      @validated.push new FON.ValidatingTextInput
        control: @host
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false
            msg: "Must specify a hostname"
          else
            ok: true
            msg: ""
        cb: @maybe_enable_join

      @validated.push new FON.ValidatingTextInput
        control: @port
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false
            msg: "Must specify a port"
          else if isNaN(text)
            ok: false
            msg: "Port must be an integer"
          else if text < 1
            ok: false 
            msg: "Port must be greater than 0"
          else
            ok: true
            msg: ""
        cb: @maybe_enable_join

      @validated.push new FON.ValidatingTextInput
        control: @password
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false
            msg: "Must specify a password"
          else
            ok: true
            msg: ""
        cb: @maybe_enable_join

      $(@el).bind "DOMNodeInsertedIntoDocument", =>
        @host.focus()
        @maybe_enable_join(@)


  class ButtonPanel extends FON.TemplateController
    template: jade["welcome_page/button_panel.jade"]

    events:
      "click a.show-create-panel": "show_create_panel"
      "click a.show-join-panel": "show_join_panel"

    elements:
      "a.create": "create"
      "#create": "create_help"

    initialize: ->
      super
      @parent = @options.parent if @options.parent
      @model.bind "change", => @render()

    show_join_panel: ->
      @parent.panel.set
        active: new JoinPanel
          model: @model
          parent: @parent
      false

    show_create_panel: ->
      if !@create.hasClass("disabled")
        @parent.panel.set
          active: new CreatePanel
            model:@model
            parent: @parent
      false

    on_render: ->
      if !@model.get "zk_cluster_service_available"
        @create.addClass("disabled")
        @create_help.html("Local ensemble service unavailable, so cannot create local ensemble server")
    

  class WelcomePage extends FON.TemplateController
    template: jade["welcome_page/index.jade"]
    elements:
      "#panel": "panel_div"

    initialize: ->
      super
      @wait = new ModalWaitDialog

      @panel = new FON.Model
      @panel.set
        active: new ButtonPanel
          model: @model
          parent: @

      @panel.bind "change:active", @render, @
      if !@model.get("managed")
        @model.bind "change:client_connected", -> window.location.reload()


    hide_wait: ->
      @wait.do_hide()
      window.location.reload()

    on_render: ->
      @panel_div.html @panel.get("active").render().el

    poll: ->
      @model.fetch
        op: "update"


  app.router.route "/welcome", "welcome", ->
    app.system_state.fetch
      success: (model, resp) ->
        if model.get "client_connected"
          app.router.navigate "/containers", true
        else
          app.menu []
          app.page new WelcomePage
            model: model
      error: (data) ->
        app.flash
          kind: "error"
          title: "Error communicating with the server"
          message: "Error fetching data from the server, error message was #{data}"
