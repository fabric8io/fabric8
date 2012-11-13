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
  "frameworks"
], ->

  class ValidatingTextInput extends FON.Controller

    initialize: ->
      super
      @control = @options.control if @options.control
      @controller = @options.controller if @options.controller
      name = @control.attr("name")
      @error_class = "##{name}"
      @error_message = "##{name}-help"

      @validator = @options.validator if @options.validator
      @cb = @options.cb if @options.cb

      @control.keyup (event) => @cb(@controller)
      @control.bind "paste", (event) => @cb(@controller)

    validate: ->
      rc = @validator(@control.val())

      if !rc.ok
        @controller.$(@error_class).addClass "error"
      else
        @controller.$(@error_class).removeClass "error"

      @controller.$(@error_message).text rc.msg
      rc.ok

  window.FON.ValidatingTextInput = ValidatingTextInput
  ValidatingTextInput
