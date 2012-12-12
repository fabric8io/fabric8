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
  "models/compute_services"
  "models/compute_providers"
  "models/nodes"
  "controllers/modal_wait_dialog"
  "controllers/controls/collection"
  "controllers/controls/validating_text_input"
  "controllers/controls/dialog"
  "controllers/controls/swappable_model_view"
], (app, jade, ComputeServices, ComputeProviders, Nodes, ModalWaitDialog) ->

  class CloudTemplate extends FON.TemplateController

    initialize: ->
      super
      @compute_providers = @options.compute_providers
      @compute_services = @options.compute_services


  class ProviderItem extends FON.TemplateController
    tagName: "li"
    className: "btn"
    template: jade["cloud_page/provider_item.jade"]
    template_data: -> @model.toJSON()

    on_render: ->
      selected = @parent.selected()
      el = $(@el)
      el.toggleClass "primary", (selected && selected.id == @model.id)

      el.click (event) =>
        @parent.selected @model
        false


  class AddComputeServiceButtons extends FON.TemplateController
    template: jade["cloud_page/add_compute_service_buttons.jade"]

    elements:
        "a.add": "add"

    events:
      "click a.add": "do_add"
      "click a.cancel": "do_cancel"

    do_add: ->
      if !@add.hasClass "disabled"
        @options.on_add() if @options.on_add
      false

    do_cancel: ->
      @options.on_cancel() if @options.on_cancel
      false


  class AddComputeService extends CloudTemplate
    template: jade["cloud_page/add_compute_service.jade"]

    elements:
      ".provider-list": "provider_list"
      ".block.right": "right_buttons"
      "input[name=identity]": "identity"
      "input[name=credential]": "credential"
      "input[name=service-id]": "service_id"
      "input[name=endpoint]": "endpoint"
      "textarea[name=options]": "opts"

    initialize: ->
      super
      @state = new FON.Model
      @selected = @state.property "selected"
      @state.bind "change:selected", @selection_changed, @

    selection_changed: ->
      old_selection = @selection
      @selection = @selected()

      old_selection.trigger("change") if old_selection
      @selection.trigger("change") if @selection

      if @selection.get("_type") == "api"
        @endpoint.val("")        
        @validated_endpoint = new FON.ValidatingTextInput
          control: @endpoint
          controller: @
          validator: (text) ->
            if !text || text == ""
              ok: false,
              msg: "You must specify the cloud API endpoint"
            else
              ok: true,
              msg: ""
          cb: @maybe_enable_add

        @endpoint.attr "disabled", false
      else
        @endpoint.val("")
        @endpoint.attr "disabled", true
        if @validated_endpoint
          @validated_endpoint.validator = (text) ->
            ok: true,
            msg: ""

      @maybe_enable_add @

    maybe_enable_add: (self) ->
      valid = true

      if self.validated_endpoint
        if !self.validated_endpoint.validate()
          valid = false

      for control in self.validated_controls
        if !control.validate() && valid
          valid = false

      if !self.selection
        valid = false

      self.buttons.add.toggleClass "disabled", !valid

    on_render: ->
      @validated_controls = []
      @validated_controls.push new FON.ValidatingTextInput
        control: @identity
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false,
            msg: "You must specify the cloud provider identity/username"
          else
            ok: true,
            msg: ""
        cb: @maybe_enable_add

      @validated_controls.push new FON.ValidatingTextInput
        control: @credential
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false,
            msg: "You must specify the cloud provider credential/password"
          else
            ok: true,
            msg: ""
        cb: @maybe_enable_add

      @identity.bind "DOMNodeInsertedIntoDocument", =>
        @identity.focus()
        @maybe_enable_add @

      providers = new FON.CollectionController
        tagName: "ul"
        collection: @compute_providers
        child_control: (model) =>
          if !model.get("configured")
            controller = new ProviderItem
              parent: @
              model: model
            model.bind "change", controller.render, controller
            controller
          else
            null

      @provider_list.html providers.render().el

      @buttons = new AddComputeServiceButtons
        parent: @
        on_cancel: => @options.on_cancel() if @options.on_cancel
        on_add: =>
          @compute_services.create_compute_service
            provider: @selected().id
            _type: @selected().get "_type"
            serviceId: @service_id.val() if @service_id.val() != ""
            endpoint: @endpoint.val() if @endpoint.val() != ""
            identity: @identity.val()
            credential: @credential.val()
            options: @opts.val()
            success: (data, textStatus, jqXHR) =>
              app.flash
                kind: "info"
                title: "Success: "
                message: "Registered new cloud provider, please wait while it initializes..."
                hide_after: 5000
                on_close: =>
                  @compute_services.fetch
                    success: (compute_services, resp) =>
                      @compute_providers.fetch
                        success: (compute_providers, resp) =>
                          app.page new CloudPage
                            compute_services: compute_services
                            compute_providers: compute_providers

            error: (model, response, options) =>
              app.flash
                kind: "error"
                title: "Error creating cloud service"
                message: "Unable to create cloud service, error message was #{response.responseText}"

          app.page new FON.LoadingPage

      @right_buttons.html @buttons.render().el

    poll: ->
      @compute_services.fetch
        op: "update"
      @compute_providers.fetch
        op: "update"



  class ProviderButtons extends CloudTemplate
    template: jade["cloud_page/provider_buttons.jade"]

    events:
      "click a.add": "do_add_service"
      "click a.delete": "do_delete_service"

    initialize: ->
      super
      @service_list = @options.service_list if @options.service_list

    do_add_service: ->
      app.page new AddComputeService
        compute_providers: @compute_providers
        compute_services: @compute_services
        on_cancel: =>
          app.page new CloudPage
            compute_providers: @compute_providers
            compute_services: @compute_services
      false

    do_delete_service: ->
      @service_list.delete_selected()
      false


  class ManagementButtons extends CloudTemplate
    template: jade["cloud_page/management_buttons.jade"]


  class InstanceItem extends FON.TemplateController
    template: jade["cloud_page/instance_item.jade"]
    template_data: -> @model.toJSON()
    elements:
      "a.terminate": "terminate"
      "a.start": "start"
      "a.stop": "stop"

    on_render: ->

      @terminate.click (event) =>
        FON.confirm(@model.get("name"), "instance", =>
          @model.destroy()
        , "destroy").render()
        false

      @start.click (event) =>
        FON.confirm(@model.get("name"), "instance", =>
          @model.start()
        , "start").render()
        false

      @stop.click (event) =>
        FON.confirm(@model.get("name"), "instance", =>
          @model.stop()
        , "stop").render()
        false


  class InstanceList extends FON.SwappableModelView

    poll_count: 0
    poll_max: 5

    do_render: -> @render()

    on_render: ->
      @nodes = new Nodes
      @nodes.url = "rest/compute_services/#{@model.id}/nodes"
      @nodes.fetch
        success: (model, resp) =>
          instances = new FON.CollectionController
            tagName: "ul"
            collection: @nodes
            child_control: (model) =>
              controller = new InstanceItem
                tagName: "li"
                className: "btn"
                parent: @
                model: model
              model.bind "change", controller.render, controller
              controller

          $(@el).html instances.render().el

    poll: ->
      @poll_count = @poll_count + 1
      if @poll_count >= @poll_max
        @poll_count = 0
        @nodes.fetch({op: "update"}) if @nodes


  class CloudServiceList extends FON.TemplateController

    initialize: ->
      super
      @model.bind "change", @render, @
      @state = new FON.Model
      @selected = @state.property "selected"
      @selected @model.at 0

      @state.bind "change:selected", @selection_changed, @
      @model.bind "remove", @element_removed, @
      @model.bind "add", @render, @

    selection_changed: ->
      old_selection = @old_selection
      @old_selection = @selected()

      @instances.set_model @old_selection

      old_selection.trigger("change") if old_selection
      @old_selection.trigger("change") if @old_selection

    element_removed: (options) ->
      if @model.length == 0
        app.page new CloudWelcomePage
          compute_services: @parent.compute_services
          compute_providers: @parent.compute_providers

      if !@selected() || @selected().id == options.id
        @selected @model.at(0)
      @render()

    on_render: ->
      services = new FON.CollectionController
        tagName: "ul"
        collection: @model
        child_control: (model) =>
          controller = new ProviderItem
            parent: @
            model: model
          model.bind "change", controller.render, controller
          controller

      $(@el).html services.render().el

    delete_selected: ->
      dialog = FON.confirm_delete @selected().get("name"), "provider", =>
        app.flash
          kind: "info"
          title: "Deleting: "
          message: "Deleting registered cloud provider"

        @selected().destroy
          success: =>
            app.flash
              kind: "info"
              title: "Success: "
              message: "Successfully deleted cloud provider"
              hide_after: 2000
          error: =>
            app.flash
              kind: "error"
              title: "Error: "
              message: "Error deleting cloud provider"
      dialog.render()


  class CloudPage extends CloudTemplate
    template: jade["cloud_page/cloud_page_body.jade"]

    elements:
      ".providers": "providers"
      ".instances": "instances"
      ".block.left": "left_buttons"
      ".block.right": "right_buttons"

    intialize: ->
      super
      @compute_services.bind "remove", =>
        if @compute_services.length == 0
          app.page new CloudWelcomePage
            compute_services: @compute_services
            compute_providers: @compute_providers


    on_render: ->
      @service_list = new CloudServiceList
        el: @providers
        model: @compute_services
        parent: @

      @instance_list = new InstanceList
        el: @instances
        parent: @
        model: @service_list.selected()

      @service_list.instances = @instance_list

      @service_list.render()
      @instance_list.render()

      left_buttons = new ProviderButtons
        el: @left_buttons
        compute_services: @compute_services
        compute_providers: @compute_providers
        service_list: @service_list
        parent: @
      left_buttons.render()

      right_buttons = new ManagementButtons
        el: @right_buttons
        compute_services: @compute_services
        compute_providers: @compute_providers
        parent: @
      right_buttons.render()

    poll: ->
      @compute_services.fetch
        op: "update"
      @compute_providers.fetch
        op: "update"
      @instance_list.poll() if @instance_list


  class CloudWelcomePage extends CloudTemplate
    template: jade["cloud_page/welcome_body.jade"]

    events:
      "click a.add": "do_add_service"

    initialize: ->
      super
      @compute_services.bind "add", =>
        if @compute_services.length > 0
          app.page new CloudPage
            compute_services: @compute_services
            compute_providers: @compute_providers

    do_add_service: ->
      app.page new AddComputeService
        compute_providers: @compute_providers
        compute_services: @compute_services
        on_cancel: => app.page new CloudWelcomePage
          compute_providers: @compute_providers
          compute_services: @compute_services
      false

    poll: ->
      @compute_services.fetch
        op: "update"
      @compute_providers.fetch
        op: "update"


  handle_error = (msg) ->
    app.flash
      kind: "error"
      title: "Error"
      message: "#{msg}"
      on_close: -> window.location.reload()

  load_page = ->

    app.page new FON.LoadingPage

    compute_services = new ComputeServices()
    compute_providers = new ComputeProviders()

    compute_providers.fetch
      success: (model, resp) ->
        compute_services.fetch
          success: (model, resp) ->
            if compute_services.length == 0
              app.page new CloudWelcomePage
                compute_services: compute_services
                compute_providers: compute_providers
            else
              app.page new CloudPage
                compute_providers: compute_providers
                compute_services: compute_services

          error: (model, resp, opts) ->
            handle_error("Failed to access compute services")
      error: (model, resp, opts) ->
        handle_error("Failed to load compute providers")


  app.router.route "/cloud", "cloud", load_page

  CloudPage
