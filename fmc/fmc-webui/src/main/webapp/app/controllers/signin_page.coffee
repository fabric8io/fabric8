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
  "controllers/controls/accordion"
], (app, jade) ->


  goto_app = ->
    app.whoami.fetch
      success: (model) ->
        username = app.whoami.get "username"
        if username && username != ""
          app.update_menu()
          app.router.navigate "/containers", true
        else
          app.flash
            kind: "error"
            title: "Invalid username or password."


  class SigninController extends FON.TemplateController
    template: jade["signin/index.jade"]
    template_data: ->
      username: "admin"

    elements:
      "input[name=\"username\"]": "username"
      "input[name=\"password\"]": "password"
      "form": "form"

    on_render: ->
      @username.bind "DOMNodeInsertedIntoDocument", => @username.focus()

      @form.submit =>

          $.ajax
            url: "/fmc/rest/system/login"
            dataType: "json"
            type: "POST"
            data:
              username: @username.val()
              password: @password.val()

            success: (data) ->
              if data
                  goto_app()
              else
                app.flash
                  kind: "error"
                  title: "Invalid username or password."
                  hide_after: 2000

            error: (data) ->
              app.flash
                kind: "error"
                title: "Error communicating with the server."

          false


  app.router.route "/signin", "signin", ->
    app.system_state.fetch
      success: (model, resp) ->
        app.menu []
        if (model.get "client_connected")
          app.page new SigninController
        else
          app.router.navigate "/welcome", true
      error: (data) ->
        app.flash
          kind: "error"
          title: "Error communicating with the server."

  app.router.route "/signout", "signout", ->
    $.ajax
      url: "/fmc/rest/system/logout.json"
      dataType: "json"
      success: (data) ->
        app.menu []
        app.page null
        app.flash
            kind: "info"
            title: "Logged out! "
            message: "Your session has been closed."
            hide_after: 2000
            on_close: ->
              app.whoami.set
                username: null
              app.router.navigate "/signin", true

      error: (data) ->
        app.whoami.fetch()
        app.flash
          kind: "error"
          title: "Error communicating with the server."

  SigninController
