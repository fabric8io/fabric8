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
  "controllers/controls/dialog"
], (app, jade) ->


  class DialogBody extends FON.TemplateController
    tagName: "fieldset"
    template: jade["agents_page/add_profile.jade"]
    elements:
      "ul.profiles": "profiles_list"

    on_render: ->
      version = app.versions.get @.model.get "version"

      existing = _.pluck(@model.get("profiles"), "id")
      # also add parents as existing profiles.

      all_profiles = version.get "_profiles"
      abstract_profiles = version.get "abstract_profiles"
      hidden_profiles = version.get "hidden_profiles"
      available = _.difference all_profiles, existing
      available = _.difference available, abstract_profiles
      available = _.difference available, hidden_profiles
      for profile in available
        @profiles_list.append("<li><div class=\"clearfix\" style=\"margin: 0px\"><label><input value=\"#{profile}\" type=\"checkbox\"><span> #{profile}</span></label></div></li>")


  class AddProfileDialog extends FON.Dialog
    accept: -> "Add"
    header: -> "Add Profiles to Container"

    process_error: (text) ->
      @show_error(text)

    on_accept: (body, options) ->
      profiles_list = body.find("ul.profiles")
      added = []
      profiles_list.find("input").each ->
        if $(@).prop("checked")
          added.push $(@).val()

      arguments =
        client_ids: added
      options = 
        success: =>
          @do_hide()
        error: (model, text, err) =>
          @process_error(err)

      @model.profiles().create arguments, options

    on_display: (body, options) ->
      controller = new DialogBody
        model: options.model
      body.append(controller.render().el)

  window.FON.AddProfileDialog = AddProfileDialog

  AddProfileDialog

