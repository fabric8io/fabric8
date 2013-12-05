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
  "controllers/controls/collection"
  "controllers/controls/dialog"
], (app, jade) ->

  class ProfileItem extends FON.ModelBackedTemplate
    template: jade["agents_page/profile_item.jade"]
    tagName: "li"
    elements:
      ".delete": "delete"
    on_render: ->
      @delete.click (event) =>
        FON.confirm_delete(@model.id, "profile", => @model.destroy()).render()
        false


  class ProfileList extends FON.CollectionController
    tagName: "ul"
    className: "profiles"
    child_control: (model) ->
      if model && model.id != "" && !model.get("_hidden")
        new ProfileItem
          model: model
      else
        null

    size: ->
      size = 0
      for id, child of @child_controls
        if @child_controls.hasOwnProperty(id)
          size = size + 1
      size

    render: ->
      if @size() > 0
        super
      else
        $(@el).html "No Associated Profiles"
        @

  ProfileList

