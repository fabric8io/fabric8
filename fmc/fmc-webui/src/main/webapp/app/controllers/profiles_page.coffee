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
  "models/versions"
  "models/profile"
  "controllers/controls/table"
  "controllers/controls/dialog"
  "controllers/controls/validating_text_input"
  "controllers/controls/collection"
  "controllers/controls/swappable_model_view"
  "controllers/controls/loading_page"
], (app, jade, Versions, Profile) ->


  class CreateVersionItem extends FON.ModelBackedTemplate
    tagName: "li"
    className: "padded"
    template: jade["profiles_page/version_item.jade"]

  class ChangeDefaultItem extends CreateVersionItem
    elements:
      "input[type='radio']": "input"

    on_render: ->
      if @model.get "_default"
        @input.attr "disabled", true
      else
        @input.removeAttr "disabled"


  class CreateNewVersionBody extends FON.TemplateController
    template: jade["profiles_page/create_version.jade"]
    elements:
      "input[name='name']": "name"
      "ul.parents": "ul"

    initialize: ->
      super
      @accept = @options.accept if @options.accept

    maybe_enable_create: (self) ->
      if !self.validated_control.validate()
        self.accept.addClass("disabled")
      else
        self.accept.removeClass("disabled")

    compare: (text) ->
      _.find @model, (p) ->
        p == text

    on_render: ->
      collection = new FON.CollectionController
        el: @ul
        collection: @model
        child_control: (model) ->
          new CreateVersionItem
            model: model

      collection.render()

      @validated_control = new FON.ValidatingTextInput
        control: @name
        controller: @
        validator: (text) =>
          regex = /^[a-zA-Z0-9_.-]*$/
          if !text || text == ""
            ok: false,
            msg: "You must specify a version name"
          else if @compare(text)
            ok: false,
            msg: "Version name must be unique"
          else if !regex.test(text)
            ok: false,
            msg: "Name can only contain letters, numbers, \u201c-\u201d, \u201c.\u201d and \u201c_\u201d "
          else
            ok: true,
            msg: ""
        cb: @maybe_enable_create

      @name.bind "DOMNodeInsertedIntoDocument", => @maybe_enable_create @



  class CreateNewVersionDialog extends FON.Dialog
    accept: -> "Create"
    header: -> "Create New Version"

    on_display: (body, options) ->
      controller = new CreateNewVersionBody
        model: @model
        accept: @accept_btn

      body.append controller.render().el

    on_accept: (body, options) ->
      id = body.find("input[name='name']").val()
      id = "<unspecified>" if id == ""

      message_id = " #{id}"
      message_id = "" if id == "<unspecified>"

      @do_hide()

      app.flash
        kind: "info"
        title: "Working: "
        message: "Creating version#{message_id}..."

      @model.create_version      
        id: id
        derived_from: body.find(":checked").val()
        success: (model, resp) =>
          app.flash
            kind: "info"
            title: "Success: "
            message: "Created version #{model.id}..."
            hide_after: 2000
        error: (model, data, resp) =>
          app.flash
            kind: "error"
            title: "Server Error: "
            message: "Version creation failed: #{data.statusText}"


  class CreateProfileItem extends FON.TemplateController
    tagName: "li"
    className: "padded"    
    template: jade["profiles_page/delete_item.jade"]
    template_data: -> @model.toJSON()
    elements:
      "input[type='checkbox']": "input"

    poll: ->
      @model.fetch
        op: "update"


  class CreateNewProfileBody extends FON.TemplateController
    tagName: "fieldSet"
    template: jade["profiles_page/add_profile.jade"]
    elements:
      ":input[name='name']": "name"
      ".parents": "parents"

    initialize: ->
      super
      @accept = @options.accept if @options.accept

    maybe_enable_create: (self) ->
      if !self.validated_control.validate()
        self.accept.addClass("disabled")
      else
        self.accept.removeClass("disabled")

    compare: (text) ->
      _.find @model, (p) ->
        p == text

    on_render: ->

      collection = new FON.CollectionController
        el: @parents
        collection: @model
        child_control: (model) ->
          if !model.get("_hidden")
            new CreateProfileItem
              model: model
          else
            null
      
      collection.render()

      @validated_control = new FON.ValidatingTextInput
        control: @name
        controller: @
        validator: (text) =>
          regex = /^[a-zA-Z0-9_-]*$/
          regex = /^[a-zA-Z0-9_.-]*$/
          if !text || text == ""
            ok: false,
            msg: "You must specify a profile name"
          else if !regex.test(text)
            ok: false
            msg: "Name can only contain letters, numbers, \u201c-\u201d, and \u201c_\u201d"
          else if @compare(text)
            ok: false,
            msg: "Profile names must be unique"
          else if !regex.test(text)
            ok: false,
            msg: "Name can only contain letters, numbers, \u201c-\u201d, \u201c.\u201d and \u201c_\u201d "
          else
            ok: true,
            msg: ""
        cb: @maybe_enable_create

      @name.bind "DOMNodeInsertedIntoDocument", => @maybe_enable_create @


  class CreateNewProfileDialog extends FON.Dialog

    accept: -> "Create"
    header: -> "Create New Profile"

    on_accept: (body, options) ->
      parents = _.map(body.find("input:checkbox").filter(":checked"), (p) -> p.value)

      profile_name = body.find(".name").val()
      version = app.session_state.get("selected_version")

      @do_hide()

      app.flash
        kind: "info"
        title: "Working: "
        message: "Creating profile #{profile_name}..."

      version.create_profile
        id: profile_name
        parents: parents
        success: (model, resp) =>
          app.flash
            kind: "info"
            title: "Success: "
            message: "Created profile #{model.id}..."
            hide_after: 2000
        error: (model, text, err) =>
          app.flash
            kind: "error"
            title: "Error: "
            message: "Failed to create profile: #{text.responseText}"

    on_display: (body, options) ->
      controller = new CreateNewProfileBody
        model: app.session_state.get("selected_version").profiles()
        accept: @accept_btn
      body.html controller.render().el


  class ImportProfileDialog extends FON.Dialog
    accept: -> "Import"
    header: -> "Import Profile"
    form_enctype: -> "multipart/form-data"
    form_action: -> "rest/versions/#{@options.model.id}/import"

    on_accept: (body, options) ->
      @do_hide()
      body.ajaxSubmit
        success: (data, textStatus, jqXHR) ->
          app.flash
            kind: "info"
            title: "Successfully imported profile"
            hide_after: 2000
        error: (data, textStatus, errorThrown) ->
          app.flash
            kind: "error"
            title: "Error importing profile: "
            message: data.responseText

    on_display: (body, options) ->
      controller = new FON.TemplateController
        template: jade["profiles_page/import_profile.jade"]

      body.html controller.render().el

    do_remove: ->
      if @submitted
        super

    on_render: ->
      super
      @dialog.unbind "hidden"


  class ImportVersionDialog extends FON.Dialog
    accept: -> "Import"
    header: -> "Import Version"
    form_enctype: -> "multipart/form-data"
    form_action: -> "rest/versions/import"

    on_accept: (body, options) ->
      @do_hide()
      body.ajaxSubmit
        success: (data, textStatus, jqXHR) ->
          app.flash
            kind: "info"
            title: "Successfully imported version"
            hide_after: 2000
        error: (data, textStatus, errorThrown) ->
          app.flash
            kind: "error"
            title: "Error importing version: "
            message: data.responseText

    on_display: (body, options) ->
      controller = new FON.TemplateController
        template: jade["profiles_page/import_version.jade"]

      body.html controller.render().el

    do_remove: ->
      if @submitted
        super

    on_render: ->
      super
      @dialog.unbind "hidden"

  class ProfileRow extends FON.TemplateController
    tagName: "li"
    className: "btn"
    template: jade["profiles_page/profile_row.jade"]
    template_data: -> @model.toJSON()
    elements:
      "a.delete": "delete"
    on_render: ->

      @delete.click =>
        FON.confirm_delete(@model.id, "profile", =>
          app.flash
            kind: "info"
            title: ""
            message: "Deleting #{@model.id}..."
            hide_after: 2000

          ids = []
          ids.push @model.id
          @parent.model.delete_profiles
            ids: ids
            error: (model, data, resp) =>
              app.flash
                kind: "error"
                title: "Server Error: "
                message: "Failed to delete profile: #{resp}"
        ).render()
        false


      el = $(@el)
      el.click (event) =>
        app.router.navigate "/versions/profiles/details/#{@parent.model.id}/#{@model.id}!features", true
        false


  class ProfileList extends FON.SwappableModelView
    poll_count: 0
    poll_max: 5

    do_render: -> @render()
    
    model_changed: ->
      @profiles = null
      super

    on_render: ->
      if !@profiles
        $(@el).html """<div class="block center"><img src="img/ajax-loader.gif"></div>"""
      @profiles = @model.profiles()
      @profiles.fetch
        success: (model, resp) =>
          profiles = new FON.CollectionController
            tagName: "ul"
            collection: model
            child_control: (model) =>
              if !model.get("_hidden")
                controller = new ProfileRow
                  tagName: "li"
                  className: "btn"
                  parent: @
                  model: model
                model.bind "change", controller.render, controller
                controller
              else
                null

          $(@el).html profiles.render().el

    poll: ->
      @poll_count = @poll_count + 1
      if @poll_count >= @poll_max
        @poll_count = 0
        @profiles.fetch {op: "update"} if @profiles


  class VersionRow extends FON.TemplateController
    template: jade["profiles_page/version_row.jade"]
    tagName: "li"
    className: "btn"

    elements:
      "a.delete": "delete"
      "a.default": "default"

    template_data: -> @model.toJSON()

    on_render: ->      

      @delete.click =>
        FON.confirm_delete(@model.id, "version", =>
          app.flash
            kind: "info"
            title: ""
            message: "Deleting #{@model.id}..."
            hide_after: 2000

          ids = []
          ids.push @model.id
          @parent.model.delete_versions
            ids: ids
            error: (model, data, resp) =>
              app.flash
                kind: "error"
                title: "Server Error: "
                message: "Failed to delete version: #{resp}"
        ).render()
        false

      @default.click =>
        app.flash
          kind: "info"
          title: ""
          message: "Changing default profile to #{@model.id}..."
          hide_after: 2000
        @parent.model.change_default
          id: @model.id
          error: (model, data, resp) =>
            app.flash
              kind: "error"
              title: "Server Error: "
              message: "Failed to change default revision: #{resp}"
        false

      selected = @parent.selected()
      el = $(@el)
      el.toggleClass "primary", (selected && selected.id == @model.id)

      el.click (event) =>
        @parent.selected @model
        false


  class VersionList extends FON.TemplateController

    initialize: ->
      super
      @state = new FON.Model
      @selected = @state.property "selected"
      if app.session_state.get("selected_version")
        @selected @model.get(app.session_state.get("selected_version").id)
      else
        @selected @model.at 0

      app.session_state.set
        selected_version: @selected()
      @old_selection = @selected()

      @state.bind "change:selected", @selection_changed, @
      @model.bind "remove", @element_removed, @

    selection_changed: ->
      old_selection = @old_selection if @old_selection
      @old_selection = @selected()

      @profiles.set_model @old_selection
      app.session_state.set
        selected_version: @selected()

      old_selection.trigger("change") if old_selection
      @old_selection.trigger("change") if @old_selection

    element_removed: (options) ->
      if !@selected() || @selected().id == options.id
        @selected @model.at(0)
        app.session_state.set
          selected_version: @selected()

    on_render: ->
      versions = new FON.CollectionController
        tagName: "ul"
        collection: @model
        child_control: (model) =>
          controller = new VersionRow
            parent: @
            model: model
          model.bind "change", controller.render, controller
          controller

      $(@el).html versions.render().el


  class ProfilesPage extends FON.TemplateController
    template: jade["profiles_page/index.jade"]
    elements:
      ".versions": "versions_table"
      ".profiles": "profiles_table"
      "a.delete-profile": "delete_profile"
      "a.delete-version": "delete_version"
      "a.change-default": "change_default"
      "a.export": "export_button"

    events:
      "click a.create-version": "do_create_version"
      "click a.create-profile": "do_create_profile"
      "click a.import-profile": "do_import_profile"
      "click a.import-version": "do_import_version"
      "click a.export": "do_export"

    selected: (value)-> 
      app.session_state.set
        selected_profile:value

    do_import_version: ->
      new ImportVersionDialog
        model: @version_list.selected()
      .render()
      false

    do_import_profile: ->
      new ImportProfileDialog
        model: @version_list.selected()
      .render()
      false

    do_create_version: ->
      new CreateNewVersionDialog
        model: @model
      .render()
      false
      
    do_create_profile: ->
      new CreateNewProfileDialog
        parent: @
      .render()
      false

    do_export: ->
      selected = app.session_state.get("selected_version")
      window.location = "rest/versions/#{selected.id}/export/#{selected.id}-export.zip"
      false

    on_render: ->
      @version_list = new VersionList
        el: @versions_table
        model: @model
        parent: @

      @profile_list = new ProfileList
        el: @profiles_table
        parent: @
        model: @version_list.selected()

      @version_list.profiles = @profile_list

      @version_list.render()
      @profile_list.render()

      
    poll: ->
      @model.fetch
        op: "update"
      @profile_list.poll() if @profile_list


  app.router.route "/versions", "versions", ->
    app.page new FON.LoadingPage
    app.versions.fetch
      success: (model, resp) =>
        if !app.session_state.get "selected_version"
          app.session_state.set
            selected_version: app.versions.default_version()

        app.page new ProfilesPage
          model: model
          
  ProfilesPage

