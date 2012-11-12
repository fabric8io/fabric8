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
  "views/jade"
], (jade)->

  #
  # new EditableProperty
  #   el: @$("name")
  #   property: @model.property("name")
  #
  class EditableProperty extends FON.TemplateController

    template: jade["editable_property_controller.jade"]
    template_data: ->
      state: @state.toJSON()
      property: @property()

    events:
      "form submit": "save"
      "click": "do_edit"
      "click a.save": "save"
      "click a.cancel": "do_cancel"
      "click a.edit": "do_edit"

    initialize: ->
      @state = new FON.Model
        editing: false
      @property = @options.property
      @on_save = @options.on_save if @options.on_save
      super
      @property.bind @render, @
      @state.bind "all", @render, @

    remove: ->
      @property.unbind @render
      super

    save: ->
      update = @$("input").val()
      @property(update)
      @on_save()
      @state.set
        editing:false
      false

    do_edit: ->
      @state.set
        editing: true
      false

    do_cancel: ->
      @state.set
        editing: false
      false

    on_save: ->
      @property.save()

  window.FON.EditableProperty = EditableProperty

  class EditableSelect extends FON.EditableProperty
    template: jade["editable_select_controller.jade"]

    initialize: ->
      super
      @state.set
        opts: @options.opts

    save: ->
      update = @$("select").val()
      @property(update)
      @on_save()
      @state.set
        editing:false
      false


  window.FON.EditableSelect = EditableSelect

  EditableProperty