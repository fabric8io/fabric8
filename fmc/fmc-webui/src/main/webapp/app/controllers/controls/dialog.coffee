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
  "frameworks"
], (app, jade) ->

  class Dialog extends FON.TemplateController
    template: jade["common/dialog.jade"]
    template_data: =>
      accept: @accept()
      cancel: @cancel()
      header: @header()
      form_action: @form_action()
      form_method: @form_method()
      form_enctype: @form_enctype()

    elements:
      "#dialog": "dialog"
      "form.body": "body"
      ".accept": "accept_btn"
      ".cancel": "cancel_btn"
      ".flash": "flash"

    accept: -> "OK"
    cancel: -> "Cancel"
    header: -> "Insert Header Here"

    form_action: -> ""
    form_method: -> "post"
    form_enctype: -> "application/x-www-form-urlencoded"

    get_options: ->
      @options

    on_display: (body, options) ->
      body.append("<p>Empty Dialog</p>")

    on_accept: (body, options) ->

    show_error: (text) ->
      controller = new FON.TemplateController
        template: jade["common/error_flash.jade"]
        template_data: =>
          message: text
      @flash.empty()
      @flash.append(controller.render().el)

    on_render: ->
      @on_display(@body, @get_options())
      @body.append("""<input type="submit" style="display:none;">""")

      @accept_btn.click (event) =>
        if !@accept_btn.hasClass "disabled"
          @do_accept()

      @cancel_btn.click (event) =>
        @do_hide()

      @dialog.bind "shown", =>
        @body.find("input[type='text']:first").focus()
        $("input").keypress (e) =>
          if e.which == 13
            if !$(e.eventTarget).hasClass("no-submit")
              false
            else
              @do_accept()
              false

      @body.submit (event) =>
        @do_accept()
        false

      @dialog.bind "hidden", =>
        @dialog.remove()

      @dialog.modal
        backdrop: true
        show: true

    do_hide: ->
      @dialog.modal('hide')
      false

    do_accept: ->
      @on_accept(@body, @get_options())
      false

  window.FON.Dialog = Dialog

  class ConfirmationDialog extends FON.Dialog
    accept: -> "Yes"
    cancel: -> "No"
    process_error: (text) =>
      @do_hide()
      app.flash
        kind: "error"
        title: "Error : "
        message: text
    initialize: ->
      @header = @options.header if @options.header
      @on_accept = @options.on_accept if @options.on_accept
      FON.Dialog.prototype.initialize.call(@)

    on_display: (body, options) ->
      text = options.body_text()
      body.append("<p>#{text}</p>")

    on_render: ->
      super
      if @options.backwards
        @accept_btn.addClass("secondary")
        @accept_btn.removeClass("primary")
        @cancel_btn.addClass("primary")
        @cancel_btn.addClass("secondary")

  window.FON.ConfirmationDialog = ConfirmationDialog

  window.FON.confirm = (id, type, on_accept, action) ->
    new FON.ConfirmationDialog
      backwards: true
      header: -> "Confirm #{_.titleize(action)}"
      body_text: -> "Are you sure you want to #{FON.escapeHtml(action)} the #{FON.escapeHtml(type)} &#8220;#{FON.escapeHtml(id)}&#8221;?"
      on_accept: ->
        on_accept()
        @do_hide()

  window.FON.confirm_delete = (id, type, on_accept) -> window.FON.confirm(id, type, on_accept, "delete")

  window.FON.dialog = (d) -> d

  Dialog




