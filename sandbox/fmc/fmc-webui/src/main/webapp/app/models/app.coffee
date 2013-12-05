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
  "models/whoami"
  "models/versions"
  "models/system_state"
  "models/os_and_versions"
], (WhoAmI, Versions, SystemState, OSAndVersions) ->

  class AppModel extends FON.Model
    defaults:
      menu: []
      loading: false
      username: ""
      flash: null
      page: null
      poll_interval: 0

  class FormModel extends FON.Model
    error_message: null

    error: (message) ->
      @.set({error_message: message})
      false

    clear: ->
      @.set({error_message: null})
      false

  class Router extends Backbone.Router

  model = new AppModel

  app =
    router: new Router()
    whoami: new WhoAmI()
    form: new FormModel
    versions: new Versions()
    cloud_os_and_versions: new OSAndVersions
    model: model
    session_state: new FON.Model
    system_state: new SystemState
    flash: model.property("flash")
    page: model.property("page")
    menu: model.property("menu")

  update_menu = ->
    menu = []

    if app.whoami.get("username") && app.whoami.get("username") != ""
      menu.push
        href: "#/containers"
        label: "Containers"
      menu.push
        href: "#/cloud"
        label: "Cloud"
      menu.push
        href: "#/versions"
        label: "Profiles"
      #menu.push
      #  href: "#/registry"
      #  label: "Registry"
      menu.push
        href: "#/patches"
        label: "Patching"
      if app.system_state.get "has_backing_engine"
        menu.push
          href: "#/users"
          label: "Users"
      menu.push
        href: "#/user_settings"
        label: "Settings"

    app.menu menu

  app.update_menu = update_menu
  app.versions.bind  "all", update_menu
  app.system_state.bind  "change:has_backing_engine", update_menu
  app.model.bind "change:poll_interval", =>
    window.set_local_storage 'poll_interval', app.model.get('poll_interval')

  app.model.set({url: window.location.hash})
  $(window).bind('hashchange', (url)->
    app.model.set({url: window.location.hash})
  )


  # Update the username when the whoami info changes..
  app.whoami.bind  "change", ->
    app.model.set
      username: app.whoami.get("username")

  app.handle_ajax_error = (resp, next)->
    if resp.status == 401
      unless _.isEmpty(app.model.get("username"))
        if !app.flash()
          app.page null
          app.flash
            kind: "error"
            title: "Unauthorized!"
            message: "You are not authorized to perform that action. Perhaps you need to log in under a different username."
            actions: "<a href='#/signin' class='btn'>Log In</a>"
            on_close: -> window.location.reload()
      else
        app.router.navigate "/signin", true
    else
      if next
        next(resp)
      else
        app.flash
          kind: "error"
          title: "Server Error"
          message: "The server is experiencing problems right now. Try again later."

  original_sync = Backbone.sync
  Backbone.sync = (method, model, options) ->
    getUrl = (object) ->
      return null  unless (object and object.url)
      (if _.isFunction(object.url) then "#{object.url()}" else "#{object.url}")

    params = _.extend({}, options)
    unless params.url
      params.url = getUrl(model) or urlError()
      parts = params.url.split("?", 2)
      parts[0] += ".json"
      params.url = parts.join("?")

    params.headers = _.extend({}, params.headers)
    params.headers["AuthPrompt"] = "false"

    original_error = params.error
    params.error = (resp) ->
      app.handle_ajax_error(resp, original_error)

    original_sync method, model, params

  window.FON.app = app

  app
