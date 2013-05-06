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
  "models/whoami"
], (app, jade, WhoAmI) ->


  class LoadingPage extends FON.TemplateController
    template: jade["application/loading.jade"]


  class FlashContainer extends FON.TemplateController
    template: jade["application/flash.jade"]
    template_data: ->
      flash: app.model.get("flash")

    elements:
      "p.block.message": "message"

    events:
      "click .close": "close"

    close: ->
      on_close = app.model.get("flash").on_close
      app.flash(null)
      on_close() if on_close
      false

    initialize: ->
      super
      app.model.bind "change:flash", @render, @

    on_render: ->
      if app.flash()

        flash = app.flash()

        if flash.hide_after
          setTimeout @close, flash.hide_after


        title = flash.title
        @message.html "<strong>#{title} </strong>"

        if flash.message
          messages = []
          message = flash.message.split("\n")

          for m in message
            messages.push m.replace(/&/g, '&amp;').replace(/>/g, '&gt;').replace(/</g, '&lt;').replace(/"/g, '&quot;').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;')

          @message.append messages.join("<br>\n")


  class Application extends FON.Controller
    page_container: $("#page_container")

    initialize: ->
      topbar_nav_container = new FON.TemplateController
        el: $("#topbar_nav_container")
        template: jade["application/menu.jade"]
        template_data: ->
          model = app.model.toJSON()
          items: model.menu
          active: model.url

      app.model.bind "change:menu", => topbar_nav_container.render()
      app.model.bind "change:url", => topbar_nav_container.render()

      topbar_nav_container.bind "render", (controller)-> bind_menu_actions controller.el

      topbar_user_container = new FON.TemplateController
        el: $("#topbar_user_container")
        template: jade["application/user_menu.jade"]
        template_data: ->
          app.model.toJSON()
        on_render: ->
          $("#app_help").click ->
            window.open "doc/help/index.html"

      topbar_user_container.bind "render", (controller)-> bind_menu_actions controller.el
      app.model.bind "change:username", topbar_user_container.render, topbar_user_container

      flash_container = new FlashContainer
        el: $("#flash_container")

      app.model.bind "change:page", =>
        @old_page.remove() if @old_page
        @old_page = app.page()
        app.flash(null)
        @page_container.empty()
        @page_container.html @old_page.render().el if @old_page

      app.model.bind "change:poll_interval", =>
        @stop_poll()
        interval = parseInt("" + app.model.get("poll_interval"))
        if interval
          @poll_interval = setInterval (=>@poll()), interval

      app.whoami.fetch();
      app.versions.fetch();
      app.cloud_os_and_versions.fetch()

      adjustHeight = ->
        windowHeight = $(window).height()
        headerHeight = $("#header").height()
        flashHeight = $("#flash_container").height()
        footerHeight = $("#footer").height()
        containerHeight = windowHeight - headerHeight - flashHeight - footerHeight - 49
        $("#page_container").css "min-height", "" + containerHeight + "px"

      $(document).ready ->
        adjustHeight()
        $(window).resize adjustHeight

    stop_poll: ->
      if @poll_interval
        clearInterval @poll_interval
        @poll_interval = null

    poll: ->
      app.system_state.fetch
        op: "update"
      page = app.page()
      page.poll()  if page and page.poll

  Application
