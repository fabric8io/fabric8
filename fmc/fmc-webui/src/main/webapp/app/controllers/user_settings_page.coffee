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
], (app, jade) ->

  class UserSettingsPage extends FON.TemplateController
    template: jade["settings_page/index.jade"]

    elements:
      "select[name=frequency]": "frequency"

    on_render: ->

      @frequency.val(app.model.get("poll_interval"));

      @frequency.bind "change", =>
        app.model.set({poll_interval: @frequency.val()})
        false





  app.router.route "/user_settings", "settings", ->
    app.page new UserSettingsPage

  UserSettingsPage
