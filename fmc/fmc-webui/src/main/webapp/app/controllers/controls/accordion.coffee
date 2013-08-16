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
  #
  # {
  #    open: boolean | default: false
  #    title: string | requried
  #    content: function Controller | required
  # }
  #
  Accordion = FON.Controller.extend(Backbone.Events).extend
    title: ""
    open: (new FON.Model()).property("open")

    initialize: ->
      @open = @options.open if @options.open
      @title = @options.title if @options.title?
      @content = @options.content if @options.content
      @open.bind @render, @

    render_part: (value)->
      switch typeof(value)
        when 'string' then value
        when 'function' then @render_part(value(@))
        else value.render().el

    remove: ->
      @open.unbind @render
      FON.Controller.prototype.remove.call(@)

    render: ->
      $(@el).empty();
      link = @make("a", {class:"accordion", href:"#"})
      @el.appendChild(link)
      link.appendChild(@make("h5", {}, @title))
      if @open()
        $(link).addClass("accordion-opened")
        $(link).click =>
          @open(false)
          false

        content_div = $(@make("div"))
        content_div.hide()
        $(@el).append(content_div)
        rendered_content = @render_part(@content)
        content_div.append(rendered_content)
        content_div.toggle "slow"
      else
        $(link).removeClass("accordion-opened")
        $(link).click =>
          @open(true)
          false
      @

  window.FON.Accordion = Accordion
  Accordion