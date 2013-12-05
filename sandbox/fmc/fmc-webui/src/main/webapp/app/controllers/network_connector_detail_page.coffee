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
  "models/activemqs"
  "models/agent"
  "controllers/controls/table"
], (app, jade, ActiveMQs, Agent) ->

  class BridgeItem extends FON.TemplateController
    tagName: "tr"
    template: jade["activemq_detail_page/bridge_row.jade"]
    template_data: -> parse_id(@model.id, @model.toJSON())


  class BridgeList extends FON.Table
    className: "zebra-striped"
    template: jade["activemq_detail_page/bridge_list.jade"]

    child_control: (model) ->
      control = new BridgeItem
        model: model
      model.bind "change", -> control.render()
      control


  class NCItem extends FON.TemplateController
    tagName: "tr"
    template: jade["activemq_detail_page/nc_row.jade"]
    template_data: -> @model.toJSON()


  class NCList extends FON.Table
    className: "zebra-striped"
    template: jade["activemq_detail_page/nc_list.jade"]

    child_control: (model) ->
      control = new NCItem
        model: model
      model.bind "change", -> control.render()
      control


  class NCDetailPage extends FON.ModelBackedTemplate
    template: jade["activemq_detail_page/network_connector_section.jade"]

    elements:
      ".connector-list": "ncs"
      ".bridge-list": "bridges"

    on_render: ->
      nc_table = new NCList
        collection: @model.network_connectors()
      bridge_table = new BridgeList
        collection: @model.network_bridges()

      @ncs.html nc_table.render().el
      @bridges.html bridge_table.render().el
  

  app.router.route "/containers/details/:name/broker/:broker/ncs", "broker_details", (name, broker) ->
    model = new Agent
      id: name
    model.fetch
      success: (model, resp) ->

        json_model = model.toJSON()

        if _.contains(json_model.extensions, "activemq")
          url = "#{model.url()}/extensions/activemq"
          brokers = ActiveMQs.singleton
            url: url
          brokers.fetch
            success: (model, resp) ->
              model = (model for model in model.models when model.get("name") == broker)[0]

              if !model
                app.router.navigate "/containers/details/#{name}/brokers", true

              model.set
                agent_name: name

              app.page new NCDetailPage
                model: model
                agent_name: name

            error: (model, resp, opts) ->
              app.router.navigate "/agent/details/#{name}/brokers", true

        else
          app.router.navigate "/agent/details/#{name}/brokers", true

      error: (model, resp, opts) ->
        app.router.navigate "/agent/details/#{name}/brokers", true

