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
  "views/charts",
  "models/osgi"
  "controllers/controls/tabs"
  "controllers/controls/table"
  "controllers/controls/label"
  "controllers/controls/collection"
  "controllers/controls/loading_page"  
], (app, jade, charts, Osgi, Tabs, table, label) ->

  class BundleDetail extends FON.ModelBackedTemplate
    tagName: "td"
    attr: "colspan=4"
    template: jade["osgi_page/bundle_details.jade"]


  class BundlesRow extends FON.TemplateController
    tagName: "tr"
    template: jade["osgi_page/bundles_row.jade"]
    template_data: ->
      json_model = @model.toJSON()
      json_model['name'] = json_model['symbolic_name']
      for header in json_model.headers
        if header.key == "Bundle-Name"
          json_model['name'] = header.value
      json_model

    on_render: =>
      el = $(@el)
      el.click (event) =>
        app.router.navigate "/containers/details/#{@parent.container}/osgi/bundles/#{@model.id}", true
        false

  class BundlesTableNavController extends FON.Table
    className: "zebra-striped bundles-list nav-list"
    template: -> jade["osgi_page/bundles_list.jade"]

    child_control: (model)->
      row = new BundlesRow
        model: model
        parent: @
      model.bind "change", row.render, row
      row

    initialize: ->
      super
      @container = @options.container if @options.container


  class BundlesController extends FON.TemplateController
    template: jade["osgi_page/bundles_tab.jade"]

    on_render: ->
      @table = new BundlesTableNavController
        collection: @model
        container: @container
      $(@el).html @table.render().el
      $(@table.el).dataTable
        "bPaginate": false,
        "bLengthChange": false,
        "bFilter": true,
        "bSort": false,
        "bInfo": false,
        "bAutoWidth": false
        "aoColumnDefs": [ 
          { "bVisible": false, "aTargets": [4] },
          { "bVisible": false, "aTargets": [5] },
          { "bVisible": false, "aTargets": [6] }
        ]


    initialize: ->
      super
      @container = @options.container


  class OsgiController extends FON.TemplateController
    poll_count: 0
    poll_max: 10

    template: jade["osgi_page/index.jade"]
    template_data: ->
      container: @container

    elements:
      "#osgi_tabs_container": "osgi_tabs_container"

    initialize: ->
      super
      @container = @options.container if @options.container

    on_render: ->
      @bundles = new BundlesController
        model: @model
        container: @container

      @osgi_tabs_container.html @bundles.render().el


    poll: ->
      @poll_count = @poll_count + 1
      if @poll_count >= @poll_max
        @poll_count = 0
        @model.fetch({op: "update"}) if @model


  class ServiceDetail extends FON.TemplateController
    tagName: "li"
    template: jade["osgi_page/service_details.jade"]
    template_data: ->
      @model.set {container: @container}, {silent: true}
      @model.toJSON()

    initialize: ->
      super
      @container = @options.container if @options.container


  class OsgiBundleDetailPage extends FON.TemplateController
    poll_count: 0
    poll_max: 10

    template: jade["osgi_page/bundle_details.jade"]
    elements:
      ".service-info": "service_info"

    initialize: ->
      super
      @services = @options.services if @options.services
      @model.bind "change", @render, @

    template_data: -> 
      @model.set
        container: @options.container
      @model.toJSON()

    on_render: ->
      services = new FON.CollectionController
        tagName: "ul"
        className: "simple"
        collection: @services
        child_control: (model) =>
          if model.get("bundle_id") == @model.id
            controller = new ServiceDetail
              parent: @
              container: @options.container
              model: model
            model.bind "change", controller.render, controller
            controller
          else
            null

      @service_info.append services.render().el

    poll: ->
      @poll_count = @poll_count + 1
      if @poll_count >= @poll_max
        @poll_count = 0
        @model.fetch
          op: "update"
        @services.fetch
          op: "update"


  app.router.route "/containers/details/:container/osgi/bundles/:id", "osgi", (container, id)->
    app.page new FON.LoadingPage
    model = new FON.Model
    model.url = "rest/agents/#{container}/extensions/osgi/bundles/#{id}"

    services = new FON.Collection
    services.url = "rest/agents/#{container}/extensions/osgi/services"

    model.fetch
      success: (model, r) ->
        services.fetch
          success: (services, r) ->
            app.page new OsgiBundleDetailPage
              model: model
              services: services
              container: container

  app.router.route "/containers/details/:container/osgi/bundles", "osgi", (container)->
    app.page new FON.LoadingPage
    model = new FON.Collection
    model.url = "rest/agents/#{container}/extensions/osgi/bundles"
    model.fetch
      success: (model, r) ->
        app.page new OsgiController
          model: model
          container: container


  OsgiController
