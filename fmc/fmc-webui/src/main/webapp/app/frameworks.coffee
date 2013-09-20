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
  "frameworks/order!frameworks/jquery"
  "frameworks/order!frameworks/underscore"
  "frameworks/order!frameworks/backbone"
#  "frameworks/order!frameworks/jquery.flot"
#  "frameworks/order!frameworks/jquery.flot.stack"
#  "frameworks/order!frameworks/jquery.flot.crosshair"
  "frameworks/order!frameworks/jquery.json-2.2"
  "frameworks/order!frameworks/jquery.effects.core"
  "frameworks/order!frameworks/jquery.effects.slide"
  "frameworks/order!frameworks/jquery.datatables"
  "frameworks/order!frameworks/underscore.string"
  "frameworks/order!frameworks/coffeejade-runtime"
  "frameworks/order!frameworks/bootstrap-dropdown"
  "frameworks/order!frameworks/bootstrap-twipsy"
  "frameworks/order!frameworks/bootstrap-popover"
  "frameworks/order!frameworks/bootstrap-modal"
  "frameworks/order!frameworks/base64"
  "frameworks/order!frameworks/jquery.form"
  "frameworks/order!frameworks/jquery.xml2json"
  # Only needed for IE support, we should just force Chrome Frame to simplify
  # cross browser support.
  # "frameworks/order!frameworks/excanvas.js"
], ->

  window.lazy = (cache_field, func) ->
    ->
      this["_" + cache_field] = func.apply(this, arguments)  unless this["_" + cache_field]
      this["_" + cache_field]

  class ClassHelpers

  _.find = _.detect

  # Wrap an optional error callback with a fallback error event.
  wrapError = (onError, model, options) ->
    (resp) ->
      if onError
        onError(model, resp, options)
      else
        model.trigger('error', model, resp, options)

  class Model extends Backbone.Model
    property: (field)->
      rc = =>
        if arguments.length >0
          atts = {}
          atts[field] = arguments[0]
          @set atts
        @get field
      rc.model = @
      rc.bind = (cb, ctx)=> @bind "change:#{field}", cb, ctx
      rc.unbind = (cb)=> @unbind "change:#{field}", cb
      rc.save = (options)=>
        options = _.extend({
          url: "#{@url()}/#{field}.json"
          type: "PUT"
          processData: false
          error: (resp)=>
            @trigger("error", @, resp)
        }, options)

        if !options.data?
          options.data = @get field
          if options.dataType == "json"
            options.data = JSON.stringify( options.data )

        $.ajax(options)
        false

      rc


  class Collection extends Backbone.Collection
    update : (models, options) ->
      models  || (models = [])
      options || (options = {})

      map = (keys, values) ->
        _.reduce( _.zip(keys, values), ((memo, v)->memo[v[0]]=v[1]; memo), {})
      new_keys = _.pluck(models, "id")
      new_models_by_id = map(new_keys, models)
      old_keys = _.pluck(@models, "id")
      old_keys_by_id = map(old_keys, @models)

      added = _.difference(new_keys, old_keys)
      removed = _.difference(old_keys, new_keys)
      updated = _.difference(old_keys, removed)

      o = {silent: options.silent || false}
      for id in updated
        @get(id).set(new_models_by_id[id], o)
      for id in removed
        @remove(old_keys_by_id[id], o)
      for id in added
        @add(new_models_by_id[id], o);

      @

    # Fetch the default set of models for this collection, resetting the
    # collection when they arrive. If `op: 'add'` is passed, appends the
    # models to the collection instead of resetting. If `op: 'update'` is
    # passed, updates the collection instead of resetting.
    fetch : (options) ->
      options || (options = {})
      collection = @;
      success = options.success;
      options.success = (resp, status, xhr) ->
        collection[options.op || 'reset'](collection.parse(resp, xhr), options)
        success(collection, resp) if (success)
      options.error = wrapError(options.error, collection, options)
      (this.sync || Backbone.sync).call(this, 'read', this, options);


  class Controller extends Backbone.View


  ExtensionHelpers =
    singleton: (extensions, options)->
      x = (this.extend(extensions))
      new x(options)

  _.extend Model, ClassHelpers
  _.extend Collection, ClassHelpers
  _.extend Controller, ClassHelpers

  _.extend Model, ExtensionHelpers
  _.extend Collection, ExtensionHelpers
  _.extend Controller, ExtensionHelpers

  (($) ->
    jQuery.event.special.destroyed =
      remove: (o) ->
        o.handler.apply(this, arguments) if o.handler

  )(jQuery)


  class PagedCollection extends Collection
    page: 1
    total: 0
    per_page: 20

    url: ->
      params = $.param
        page: @page
        per_page: @per_page
      "#{@baseUrl}?#{params}"

    parse: (data) ->
      @page = data.page
      @per_page = data.per_page
      @total = data.total
      data.models

    pages: -> @total / @per_page

    next_page: ->
      @page = @page + 1
      @fetch()

    prev_page: ->
      @page = @page - 1
      @fetch()


  class TemplateController extends Controller

    initialize: ->
      super
      _.extend this, Backbone.Events.prototype

      (@attr = @options.attr) if @options.attr
      (@elements = @options.elements) if @options.elements
      (@template = @options.template) if @options.template
      (@on_render = @options.on_render) if @options.on_render
      (@template_data = @options.template_data) if @options.template_data
      (@parent = @options.parent) if @options.parent
      (@options.initialize()) if @options.initialize
      (@bind "render", @on_render, @) if @on_render

    render: ->
      $(@el).attr(@attr) if @attr
      if @template
        data = if @template_data
            @template_data(@)
          else
            {}
        # try
          contents = @template(data)
          $(@el).html contents
        # catch e
        #   console.warn("Render failure:", e, " of data: ", data, " and template:", @template)

      if @elements
        _.each @elements, (field, selector) =>
          @[field]=@$(selector)

      @trigger "render", @

      for element in $(@el).find("[title]")
        el = $(element)
        el.twipsy
          placement: "right"
          html: true
          animate: false

        el.bind 'destroyed', () ->
          $(".twipsy").remove()
          false

      @


  window.FON =
    ClassHelpers:ClassHelpers
    Model:Model
    Collection:Collection
    PagedCollection:PagedCollection
    Controller:Controller
    TemplateController:TemplateController

    model: (options,extensions) ->
      if extensions
        x = Model.extend(extensions)
        new x(options)
      else
        new Model(options)

    template: (options, extensions) ->
      if extensions
        x = TemplateController.extend(extensions)
        new x(options)
      else
        new TemplateController(options)

    model_backed_template: (options, extensions) ->
      controller = FON.template(_.extend({template_data: -> @model.toJSON()},options), extensions)
      controller.model.bind "change", -> controller.render()
      controller

    nested_collection: (name, model)->
      lazy(name, ->
        triggered = false
        url = if typeof(@url)=="function" then @url() else @url
        rc = Collection.singleton
          model: model
          url: url+"/"+name
        rc.reset(@toJSON()[name])

        # Update the nested collection if the model's attribute changes .
        @bind "change:"+name, =>
          unless triggered # avoids update loops
            triggered = true
            rc.update(@toJSON()[name])
            triggered = false

        # Update the model's attribute if the nested collection changes.
        rc.bind "all", =>
          unless triggered # avoids update loops
            triggered = true
            r = {}
            r[name]=rc.toJSON()
            @set(r)
            triggered = false
        rc
      )

    escapeHtml: (str) ->
      div = document.createElement 'div'
      div.appendChild(document.createTextNode(str))
      div.innerHTML

    unescapeHtml: (escapedStr) ->
      div = document.createElement 'div'
      div.innerHtml = escapedStr
      child = div.childNodes[0]
      if !child
        ''
      else
        child.nodeValue



  class ModelBackedTemplate extends FON.TemplateController
    template_data: -> @model.toJSON()

    initialize: ->
      super
      @model.bind "change", @render, @

    poll: ->
      @model.fetch
        op: "update"

  window.FON.ModelBackedTemplate = ModelBackedTemplate

  _.templateSettings =
    evaluate: /\<\%([\s\S\\n\\r]+?)\%\>/g
    interpolate: /\{\{([\s\S\\n\\r]+?)\}\}/g

  KB = 1024
  MB = KB * 1024
  GB = MB * 1024
  TB = GB * 1024
  SECONDS = 1000
  MINUTES = 60 * SECONDS
  HOURS = 60 * MINUTES
  DAYS = 24 * HOURS
  YEARS = 365 * DAYS

  window.as_memory = (value) ->
    if (value == 1)
      _.sprintf "%d byte", value
    else if value < KB
      _.sprintf "%d bytes", value
    else if value < MB
      _.sprintf "%.2f KB", value / KB
    else if value < GB
      _.sprintf "%.2f MB", value / MB
    else if value < TB
      _.sprintf "%.2f GB", value / GB
    else
      _.sprintf "%.2f TB", value / TB

  window.duration = (value) ->
    if value < SECONDS
      _.sprintf "%d ms", value
    else if value < MINUTES
      if (value / SECONDS) < 2
        _.sprintf "%d second", value / SECONDS
      else
        _.sprintf "%d seconds", value / SECONDS
    else if value < HOURS
      if (value / MINUTES) < 2
        _.sprintf "%d minute", value / MINUTES
      else
        _.sprintf "%d minutes", value / MINUTES
    else if value < DAYS
      if (value / HOURS) < 2
        _.sprintf "%d hour %s", value / HOURS, duration(value % HOURS)
      else
        _.sprintf "%d hours %s", value / HOURS, duration(value % HOURS)
    else if value < YEARS
      if (value / DAYS) < 2
        _.sprintf "%d day %s", value / DAYS, duration(value % DAYS)
      else
        _.sprintf "%d days %s", value / DAYS, duration(value % DAYS)
    else
      if (value / YEARS) < 2
        _.sprintf "%d years %s", value / YEARS, duration(value % YEARS)
      else
        _.sprintf "%d years %s", value / YEARS, duration(value % YEARS)

  window.duration_since = (value) ->
    duration new Date().getTime() - value

  window.bind_accordion_actions = (root) ->
    (root or $("body")).find(".accordion").click(->
      $(this).next().toggle "slow"
      $(this).toggleClass "accordion-opened"
      false
    ).each ->
      next = $(this).next()
      if next.is(":visible") && !next.is(".hide")
        $(this).toggleClass "accordion-opened"

  window.bind_menu_actions = (root) ->
    $(root).each ->
      unless $(@).data('menu_dropdown')
        $(@).data('menu_dropdown', true)
        $(root).dropdown()

  window.parse_id = (id, json) ->
    ids = id.split(",")

    for id in ids
      name_value = id.split("=")
      json[name_value[0]] = name_value[1]
    json

  window.atob = base64.decode if !window.atob
  window.btoa = base64.encode if !window.btoa


  window.supports_html5_storage = ->
    return typeof(Storage) != "undefined"

  window.set_local_storage = (key, val) ->
    if supports_html5_storage
      localStorage[key] = val

  window.get_local_storage = (key, def) ->
    if supports_html5_storage
      rc = localStorage[key]
      if !rc
        set_local_storage key, def
        def
      else
        rc
    else
      def






