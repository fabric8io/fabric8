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
  "models/profile"
  "models/versions"
  "controllers/controls/label"
  "controllers/controls/collection"
  "controllers/controls/tabs"
  "controllers/controls/dialog"
  "controllers/controls/editable_property"  
], (app, jade, Profile, Versions) ->

  class ChangeParentProfilesDialog extends FON.Dialog
    accept: -> "Change"
    header: -> "Change Parent Profiles"

    on_accept: (body, options) ->
      selected_ids = _.pluck(body.find("input[type=checkbox]").filter(":checked"), "value")

      options.model.set_parents
        parents: selected_ids
        error: (jqXHR, textStatus, errorThrown)  ->
          app.flash
            kind: "error"
            title: "Error: "
            message: "Failed to save new parent profiles: #{textStatus.responseText}"

      @do_hide()

    on_display: (body, options) ->
      controller = new FON.TemplateController
        tagName: "fieldset"
        model: options.model
        template: jade["agents_page/add_profile.jade"]
        elements:
          "ul.profiles": "profiles_list"
        on_render: ->
          version = app.versions.get @model.get "version"
          existing = @model.get("parents")

          all_profiles = _.without(version.get("_profiles"), @model.id)
          for profile in all_profiles
            checked = ""
            if _.contains(existing, profile)
              checked = "checked"
            @profiles_list.append("""<li><label><input value="#{profile}" type="checkbox" #{checked}><span> #{profile}</span></label></li>""")

      body.append controller.render().el

  class EditConfigDialog extends FON.Dialog
    accept: -> "Save"
    header: -> "View/Edit Configuration File"

    on_accept: (body, options) ->
      data = body.find(".data").val()
      encoded_data = btoa(data)
      options.model.set
        value: encoded_data
      options.model.save()
      @do_hide()

    on_display: (body, options) ->
      controller = new FON.TemplateController
        tagName: "fieldset"
        model: options.model
        template: jade["profiles_page/detail_page/edit_dialog.jade"]
        elements:
          ".data": "data"
        on_render: ->
          encoded_data = @model.get('value')
          @data.val window.atob(encoded_data)
      body.append(controller.render().el)


  class TabContentController extends FON.TemplateController
    initialize: ->
      super
      @addPromptLabel = @options.addPromptLabel
      @model.bind "change", @render, @
  
    template: jade["profiles_page/detail_page/list.jade"]
    template_data: -> 
        addPromptLabel: @addPromptLabel
    elements:
      "ul": "ul"
      "a#add": "add_button"
      "input#add": "add_input"
      "span#add": "add_label"

    on_render: ->
      collection = new FON.CollectionController
        el: @ul
        collection: @model
        child_control: (model) ->
          FON.model_backed_template
            model: model
            tagName: "li"
            template: jade["profiles_page/detail_page/pid_line.jade"]
            elements:
              ".delete": "delete"
              ".view": "view"
            on_render: (controller) ->
              controller.delete.click (event) ->
                FON.confirm_delete(model.id, "item", -> model.destroy()).render()
                false
              controller.view.click (event) ->
                e = new EditConfigDialog
                  model: model
                e.render()
                false

      @add_button.click (event) =>
        if @add_input.val() != ""
          @do_add(@add_input.val())
      @add_input.keydown (event) =>
        if event.which == 13
          @do_add(@add_input.val())
        else
          true
      setTimeout(
        =>
          # do this after stuff is in the DOM
          parent = @add_input.parent()
          parentWidth = parent.width()
          labelWidth = @add_label.width()
          buttonWidth = @add_button.width()
          inputWidth = parentWidth - labelWidth - buttonWidth - 47
          @add_input.width(inputWidth)
        , 0)

    do_add: (item) ->
      @model.create(
        id: item
        value: ""
      )
      @add_input.val("")
      true


  class AgentListController extends FON.TemplateController
    template: _.template("""<div class="edit-list"><ul></ul></div>""")
    elements:
      "ul": "ul"

    on_render: ->
      collection = new FON.CollectionController
        el: @ul
        collection: @model.agents()
        child_control: (model) ->
          FON.model_backed_template
            model: model
            tagName: "li"
            template: _.template('<a href=#/containers/{{id}}>{{FON.escapeHtml(id)}}</a>')


  class ValueListEntry extends FON.TemplateController
    tagName: "li"
    template: jade["profiles_page/detail_page/value_list_entry.jade"]
    template_data: -> @model.toJSON()
    elements:
      ".delete": "delete"
      ".val": "value"

    initialize: ->
      super
      @collection = @options.collection if @options.collection

    on_render: ->

      ep = new FON.EditableProperty
        el: @value
        property: @model.property("value")
        on_save: => 
          @collection.remove(@model)
          @collection.create
            id: @model.id
            value: @model.get("value")

      ep.render()

      @delete.click =>
        FON.confirm_delete(@model.get("value"), "item", => @model.destroy()).render()
        false


  class IDValueListEntry extends FON.TemplateController
    tagName: "li"
    template: jade["profiles_page/detail_page/id_value_list_entry.jade"]
    template_data: ->
      id = @model.id
      id = id.replace(@options.prefix, "")
      {
        value: @model.get("value")
        id: id
      }

    elements:
      ".delete": "delete"
    on_render: ->
      @delete.click =>
        FON.confirm_delete(@model.get("value"), "item", => @model.destroy()).render()
        false


  class FeatureRepositoryDisplay extends FON.TemplateController
    template: jade["profiles_page/detail_page/feature_repo.jade"]
    template_data: -> 
      installed_features = @model.features().toJSON()
      installed_repos = _.map(@parent.repos.toJSON(), (repo) -> repo.id)
      {
        installed_features: _.map(installed_features, (model) -> model.value)
        installed_repos: installed_repos
        error: @error if @error
        features: @json if @json
      }

    elements:
      ".feature-detail": "detail_container"
      "a.add-feature": "add_feature"
      "a.add-repo": "add_repo"
      "a.view-feature": "view_feature"

    initialize: ->
      super
      @json = @options.json if @options.json
      @error = @options.error if @options.error
      @repo = @options.repo if @options.repo

      @model.features().bind "add", @render, @
      @model.features().bind "remove", @render, @

    on_render: ->
      @add_feature.click (event) =>
        feature = event.currentTarget.id.substring(3)
        @model.features().create
          id: "feature.#{feature}"
          value: feature
        false

      @add_repo.click (event) =>
        repo = event.currentTarget.id.substring(3)
        repo = repo.trim()
        id = "repository.#{repo.replace(/\//g, "_")}"
        @model.repositories().create(
          id: id
          value: repo
        )
        false

      @view_feature.click (event) =>
        feature = event.currentTarget.id.substring(4)
        for f in @json.feature
          if f.name == feature
            if f.feature && !_.isArray(f.feature)
              f.feature = [f.feature]
            if f.bundle && !_.isArray(f.bundle)
              f.bundle = [f.bundle]
            if f.config
              if !_.isArray(f.config)
                f.configs = [f.config]
              else
                f.configs = f.config

            offset = $(event.currentTarget).offset()
            offset.top = offset.top - 250 - Math.floor(event.currentTarget.offsetHeight / 2) + 3
            offset.left = offset.left - 500 - event.currentTarget.offsetWidth

            @show_detail FON.template
              template: jade["profiles_page/detail_page/feature_details.jade"]
              template_data: => _.extend(f, {offset: offset})

        false

    show_detail: (controller) ->
      @detail_container.empty()
      if controller
        @detail_container.append controller.render().el
        controller.$("a.close").click =>
          @show_detail(null)  
          false      



  class FeaturesListController extends FON.TemplateController
    template: jade["profiles_page/detail_page/features_list.jade"]
    elements:
      ".edit-list": "list"
      "select[name=selected-repo]": "select"
      ".feature-list": "feature_list"

    initialize: ->
      super
      @repos = new FON.Collection
      @repos.url = "rest/versions/#{@model.get("version")}/profiles/#{@model.id}/available_repos"
      @repos.bind "add", @update_select, @
      @repos.bind "remove", @update_select, @
      @repos.fetch
        success: (model, resp) => @render()

    update_select: ->
      if @select
        @select.html("")
        @repos.each (repo) =>
          @select.append """<option value=#{repo.id}>#{repo.id}</option>"""
        if @selected_repo
          @select.val @selected_repo
          @selection_changed()
        else
          @select.val @repos.get(0).id if @repos.get(0)
          @selection_changed()

    selection_changed: ->
      @selected_repo.unbind("change", @selection_changed) if @selected_repo
      @selected_repo = @repos.get @select.val()

      if @selected_repo

        json = $.xml2json(@selected_repo.get("xml")) if @selected_repo.get("xml")

        if json
          if json.repository && !$.isArray(json.repository)
            json.repository = [json.repository]
          if json.feature && !$.isArray(json.feature)
            json.feature = [json.feature]

        @display = new FeatureRepositoryDisplay
          parent: @
          model: @model
          repo: @selected_repo
          json: json
          error: @selected_repo.get("error")

        @selected_repo.bind "change", @selection_changed, @

        @feature_list.html @display.render().el


    on_render: ->
      collection = new FON.CollectionController
        tagName: "ul"
        collection: @model.features()
        child_control: (model) =>
          new ValueListEntry
            collection: @model.features()
            model: model

      @list.html collection.render().el
      @select.change => @selection_changed()
      @update_select()

    poll: ->
      @model.fetch
        op: "update"
      @repos.fetch
        op: "update"


  class ListController extends FON.TemplateController
    template: jade["profiles_page/detail_page/list.jade"]
    template_data: -> 
        addPromptLabel: @addPromptLabel

    elements:
      "ul": "ul"
      "a#add": "add_button"
      "input#add": "add_input"
      "span#add": "add_label"

    get_child_control: (model) ->
      if @show_id
        new IDValueListEntry
          model: model
          prefix: @prefix
      else
        new ValueListEntry
          collection: @collection()
          model: model

    on_render: ->
      collection = new FON.CollectionController
        el: @ul
        collection: @collection()
        child_control: (model) => @get_child_control(model)

      @add_button.click (event) =>
        if @add_input.val() != ""
          @do_add(@add_input.val())
      @add_input.keydown (event) =>
        if event.which == 13
          @do_add(@add_input.val())
        else
          true
      setTimeout(
        =>
          # do this after stuff is in the DOM
          parent = @add_input.parent()
          parentWidth = parent.width()
          labelWidth = @add_label.width()
          buttonWidth = @add_button.width()
          inputWidth = parentWidth - labelWidth - buttonWidth - 47
          @add_input.width(inputWidth)
        , 0)

    do_add: (item) ->
      @on_add(item)
      @add_input.val("")
      false


  class ProfileVersionAndParents extends FON.ModelBackedTemplate
    template: jade["profiles_page/detail_page/parent_block.jade"]


  class ProfileAttributes extends FON.TemplateController
    attr:
      "style": "padding-top: 9px;"
    template: jade["profiles_page/detail_page/profile_attributes.jade"]
    template_data: -> @model.toJSON()

    elements:
      ":input[name=locked]": "locked"
      ":input[name=abstract]": "abstract"

    initialize: ->
      super
      @model.bind "change:_locked", =>
        @locked.attr "checked", @model.get("_locked")

      @model.bind "change:_abstract", =>
        @abstract.attr "checked", @model.get("_abstract")

    on_render: ->
      @locked.attr "checked", @model.get("_locked")
      @abstract.attr "checked", @model.get("_abstract")

      @locked.change (event) =>
        @model.set_attribute
          key: "locked"
          value: @locked.is(":checked")
          error: ->
            app.flash
              kind: "error"
              title: "Error: "
              message: "Error modifying profile attribute"
        false

      @abstract.change (event) =>
        @model.set_attribute
          key: "abstract"
          value: @abstract.is(":checked")
          error: ->
            app.flash
              kind: "error"
              title: "Error: "
              message: "Error modifying profile attribute"
        false


  class ProfileDetailController extends FON.TemplateController
    template: jade["profiles_page/detail_page/index.jade"]
    template_data: ->
      name: @options.name
      version: @options.version
      model: @model.toJSON()

    elements:
      "#tabs": "tabs"
      ".parent-block": "parent_detail_block"
      ".profile-attributes": "profile_attributes"

    events:
      "click a.set-parents": "do_set_parents"

    initialize: ->
      super
      @parent_block = new ProfileVersionAndParents
        model: @model

      @attributes_block = new ProfileAttributes
        model: @model

    do_set_parents: ->
      dialog = new ChangeParentProfilesDialog
        model: @model
      dialog.render()
      false

    on_render: ->
      name = @options.name
      version = @options.version
      @tabs_controller = new FON.Tabs
        el: @tabs
        tab: @options.tab
        tabs:
          features:
            route: "#/versions/profiles/details/#{version}/#{name}!features"
            label: new FON.Label
              model:@model
              template: (model)-> "Features (#{model.features.length})"
            page: new FeaturesListController
                    model: @model
          fabs:
            route: "#/versions/profiles/details/#{version}/#{name}!fabs"
            label: new FON.Label
              model: @model
              template: (model)-> "FABs (#{model.fabs.length})"
            page: => ListController.singleton
              addPromptLabel: "Add new FAB (example: mvn:com.foo/myfab/1.0):"
              collection: => @model.fabs()
              on_add: (items) =>
                for item in items.split(",")
                  do (item) =>
                    item = item.trim()
                    if item != ""
                      @model.features().create(
                        id: "fab.#{item}"
                        value: item
                      )
          bundles:
            route: "#/versions/profiles/details/#{version}/#{name}!bundles"
            label: new FON.Label
              model:@model
              template: (model)-> "Bundles (#{model.bundles.length})"
            page: => ListController.singleton
              addPromptLabel: "Add new bundle (example: mvn:com.foo/mybundle/1.0):"
              collection: => @model.bundles()
              on_add: (items) =>
                for item in items.split(",")
                  do (item) =>
                    item = item.trim()
                    if item != ""
                      @model.bundles().create(
                        id: "bundle.#{item}"
                        value: item
                      )

          repositories:
            route: "#/versions/profiles/details/#{version}/#{name}!repositories"
            label: new FON.Label
              model:@model
              template: (model)-> "Repositories (#{model.repositories.length})"
            page: => ListController.singleton
              addPromptLabel: "Add repository (example: mvn:com.foo/myrepo/xml/features):"
              collection: => @model.repositories()
              on_add: (item) =>
                id = "repository.#{item.replace(/\//g, "_")}"
                item = item.trim()
                @model.repositories().create(
                  id: id
                  value: item
                )

          config_props:
            route: "#/versions/profiles/details/#{version}/#{name}!config_props"
            label: new FON.Label
              model:@model
              template: (model)-> "Config Properties (#{model.config_props.length})"
            page: => ListController.singleton
              show_id: true
              prefix: "config."
              addPromptLabel: "Add new entry to config.properties (example: name=value):"
              collection: => @model.config_props()
              on_add: (items) =>
                id = _.strLeft(items, "=")
                id = _.trim(id)
                value = _.strRight(items, "=")
                value = _.trim(value)
                @model.config_props().create
                  id: "config.#{id}"
                  value: value

          system_props:
            route: "#/versions/profiles/details/#{version}/#{name}!system_props"
            label: new FON.Label
              model:@model
              template: (model)-> "System Properties (#{model.system_props.length})"
            page: => ListController.singleton
              show_id: true
              prefix: "system."
              addPromptLabel: "Add new entry to system.properties (example: name=value):"
              collection: => @model.system_props()
              on_add: (items) =>
                id = _.strLeft(items, "=")
                id = _.trim(id)
                value = _.strRight(items, "=")
                value = _.trim(value)
                @model.system_props().create
                  id: "system.#{id}"
                  value: value

          configurations:
            route: "#/versions/profiles/details/#{version}/#{name}!configurations"
            label: new FON.Label
              model:@model
              template: (model)-> "Config Files (#{model.configurations.length})"
            page: => new TabContentController
              addPromptLabel: "Add new config file (example: com.foo.myservice.properties):"
              model: @model.configurations()

      @tabs_controller.render()
      @parent_detail_block.html @parent_block.render().el
      @profile_attributes.html @attributes_block.render().el

    poll: ->
      @model.fetch
        op: "update"

      if typeof @tabs_controller.active_item.page.poll == 'function'
        @tabs_controller.active_item.page.poll()

  app.router.route "/versions/profiles/details/:version/:name", "profile_details", (version, name) ->
    app.router.navigate "/versions/profiles/details/#{version}/#{name}!features", true

  app.router.route "/versions/profiles/details/:version/:name!:tab", "profile_details", (version, name, tab) ->
    model = new Profile
    model.url = "#{app.versions.url}/#{version}/profiles/#{name}"
    model.fetch
      success: (model, resp) =>
        app.page new ProfileDetailController
          model: model
          version: version
          name: name
          tab: tab
      error: (model, resp, opts) =>
        app.flash
          kind: "error"
          title: "Error: "
          message: "Error fetching data for profile #{name}"

  ProfileDetailController
