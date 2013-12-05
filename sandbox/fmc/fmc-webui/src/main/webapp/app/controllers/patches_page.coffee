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
  "models/patch_files"
  "controllers/controls/loading_page"
], (app, jade, Versions, PatchFiles) ->

  class VersionList extends FON.TemplateController


  class PatchPage extends FON.TemplateController
    template: jade["patch_page/index.jade"]
    template_data: -> 

    elements:
      "select[name=version-select]": "version_select"
      "input[name=patch_file]": "file_input"
      ".patch-list": "patch_list"
      "form": "upload_form"
      "a.upload.btn": "upload_button"
      "a.apply.btn": "apply_button"

    events:
      "click a.apply.btn": "do_apply"

    initialize: ->
      super
      @versions = @options.versions if @options.versions
      @version = @options.version if @options.version
      @patch_files = @options.patch_files if @options.patch_files

      @patch_files.bind 'remove', @patch_files_changed, @
      @patch_files.bind 'add', @patch_files_changed, @

    patch_files_changed: ->
      if @apply_button
        @apply_button.toggleClass 'disabled', @patch_files.length == 0

    poll: ->
      @versions.fetch
        op: "update"
      @patch_files.fetch
        op: "update"

    do_apply: ->
      if @apply_button.hasClass('disabled')
        return false

      arguments =
        target_version: @version.id

      options = 
        success: (data, textStatus, jqXHR) =>
          app.flash
            kind: "info"
            title: "Patching Successful: "
            message: "Successfully applied patches to new version #{data}, use the \"Migrate Containers\" button to move containers to version #{data}."
            on_close: -> app.router.navigate "/containers", true
        error: (data, textStatus, jqXHR) ->
          app.flash
            kind: "error"
            title: "Error Patching: "
            message: data.responseText
            on_close: -> window.location.reload()

      app.page new FON.LoadingPage
      @patch_files.apply_patches arguments, options

      false


    on_render: ->

      @apply_button.toggleClass 'disabled', @patch_files.length == 0

      @upload_button.toggleClass 'disabled', @file_input.val() == ""

      @file_input.change (event) =>
        @upload_button.toggleClass 'disabled', @file_input.val() == ""

      @upload_button.click =>
        if @upload_button.hasClass('disabled')
          return false;

        app.flash
          kind: "info"
          title: "Uploading patch file to server"

        @upload_form.ajaxSubmit
          success: (data, textStatus, jqXHR) =>
            app.flash
              kind: "info"
              title: "Successfully uploaded patch file"
              hide_after: 2000

          error: (data, textStatus, errorThrown) =>
            @file_input.val ""
            @upload_button.addClass 'disabled'
            app.flash
              kind: "error"
              title: "Error uploading patch file: "
              message: data.responseText
        false


      version_control = new FON.CollectionController
        el: @version_select
        collection: @versions
        child_control: (model) ->
          FON.model_backed_template
            model: model
            tagName: "option"
            attr:
              "value": model.id
            template: _.template("#{FON.escapeHtml(model.id)}")
        on_render: =>
          super
          if @versions.get(@version.id)
            @version_select.val(@version.id)
          else
            @version = @versions.default_version()
            @version_select.val(@versions.default_version().id)

      version_control.render()

      @version_select.change =>
        @version = @versions.get(@version_select.val())

      file_listing = new FON.CollectionController
        el: @patch_list
        collection: @patch_files
        child_control: (model) ->
          FON.model_backed_template
            model: model
            tagName: "li"
            template: _.template("""<a href="#" class="delete"><img src="img/x-16.png"></a>#{FON.escapeHtml(model.id)}""")
            elements:
              ".delete": "delete"
            on_render: (controller) ->
              controller.delete.click (event) ->
                FON.confirm_delete(model.id, "patch", -> model.destroy()).render()
                false



      file_listing.render()


  handle_error = ->
    app.flash
      kind: "error"
      title: "Error: "
      message: "Failed to retrieve data from server"
      on_close: -> window.location.reload()

  app.router.route "/patches", "patches", ->
    app.page new FON.LoadingPage

    versions = new Versions
    versions.fetch
      success: (model, r) ->
        patch_files = new PatchFiles
        patch_files.fetch
          success: (m, r) ->
            app.page new PatchPage
              versions: versions
              version: versions.default_version()
              patch_files: patch_files
          error: handle_error

      error: handle_error
