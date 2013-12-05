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
  "models/app",
  "views/jade",
  "models/user",
  "models/users",
  "controllers/controls/collection"
  "controllers/controls/table"
  "controllers/controls/dialog"
], (app, jade, User, Users) ->

  class UsersTable extends FON.Table
    row_template: jade["users_page/user_row.jade"]

    initialize: ->
      @parent = @options.parent
      super

    child_control: (model)->
      controller = new FON.TemplateController
        model: model
        tagName: "tr"
        template: @row_template
        template_data: -> model.toJSON()
        on_render: (self)=>

          el = $(self.el)
          if @parent.state.get("selected") == model
            el.addClass("selected")
          else
            el.removeClass("selected")

          el.click (event)=>
            @parent.selected(model)
            false

      @parent.state.bind "change:selected", -> controller.render()
      model.bind "all", -> controller.render()
      controller


  class UsersController extends FON.TemplateController
    template: jade["users_page/index.jade"]

    selected: (value)-> @state.set({selected:value})

    initialize: ->
      super

      @state = new FON.Model

      @state.bind "change:selected", (value)=>
        @details.empty()
        @overview = null
        selected = @state.get("selected")
        if selected
          @overview = new UserOverviewController
            model: selected

          el = $(@overview.render().el)
          @details.append el
          @add_role.removeClass "disabled"
          @delete.removeClass "disabled"
          @change_password.removeClass "disabled"
        else
          @add_role.addClass "disabled"
          @delete.addClass "disabled"
          @change_password.addClass "disabled"

    elements:
      "div.nav_buttons > ul": "items"
      "a.add-role": "add_role"
      "a.change-password": "change_password"
      "a.delete": "delete"
      "#user_overview_container": "details"

    events:
      "click a.add": "do_add"
      "click a.add-role": "do_add_role"
      "click a.delete": "do_delete"
      "click a.change-password": "do_change_password"

    do_add: ->
      d = new AddUserDialog
        parent: @
        model: @model
      d.render()
      false

    do_add_role: ->
      if @state.get("selected")
        d = new AddRoleDialog
          model: @state.get "selected"
          selected: @state.get "selected"
        d.render()
      false

    do_change_password: ->
      if @state.get("selected")
        d = new ChangePasswordDialog
          model: @state.get "selected"
          selected: @state.get "selected"
        d.render()
      false

    do_delete: ->
      FON.confirm_delete(@state.get("selected").get("id"), "user", => 
        @state.get("selected").destroy()
        @state.set
          selected: null
      ).render()
      false

    on_render: ->
      @model.trigger "reset", @model
      @table = new UsersTable
        el: @$("#users")
        parent: @
        collection: @model
      @table.render()

  class RoleEntry extends FON.ModelBackedTemplate
    tagName:"li"

    template: _.template("""<a href="#" class="delete-role" title="Delete role"><img src="img/x-16.png"></a>{{FON.escapeHtml(id)}}""")
    elements:
      "a.delete-role": "delete"

    on_render: ->
      @delete.click (event) =>
        FON.confirm_delete(@model.id, "role", => @model.destroy()).render()
        false


  class UserOverviewController extends FON.TemplateController
    template: jade["users_page/user_overview.jade"]
    template_data: ->  @model.toJSON()
    elements:
      "ul.roles": "ul_roles"
    on_render: ->

      ul = new FON.CollectionController
        el: @ul_roles
        collection: @model.roles()
        child_control: (model) ->
          new RoleEntry
            model: model
      ul.render()


  class AddUserDialogBody extends FON.TemplateController
    tagName: "fieldset"
    template: jade["users_page/add_user.jade"]


  class AddUserDialog extends FON.Dialog
    accept: -> "Create"
    header: -> "Create New User"

    on_accept: (body, options) ->
      username = body.find("#username_input").val()

      if (username.length < 5)
        @show_error("Username must be at least 5 characters long")
        return

      for user in @model.models
        if username == user.id
          @show_error("Username already exists")
          return

      password = body.find("#password_input").val()
      repeat_password = body.find("#repeat_password_input").val()
      if (password.length < 5)
        @show_error("Password must be at least 5 characters long")
        return
      if (password != repeat_password)
        @show_error("Passwords don't match")
        return


      user = new User()

      user.id = username
      user.password = password
      user.save {password: password}
        error: =>
          @do_hide()
          app.flash
            kind: "error"
            title: "Error adding user."
        success: =>
          @do_hide()
          @model.fetch()

    on_display: (body, options) ->
      controller = new AddUserDialogBody
        model: options.model

      body.html controller.render().el  


  class AddRoleDialog extends FON.Dialog
    accept: -> "Add"
    header: -> "Add New Role"

    initialize: ->
      @model = @options.model if @options.model
      super

    on_accept: (body, options) ->
      role = body.find("#role_input").val()

      if (role.length < 4)
        @show_error("Role must be at least 4 characters long")
        return

      @model.roles().create(
        "id": role
      ,
        success: =>
          @do_hide()
          @model.collection.fetch()
        error: =>
          @do_hide()
          app.flash
            kind: "error"
            title: "Error adding role"
      )

    on_display: (body, options) ->
      body.append(new FON.TemplateController
          tagName: "div"
          template: jade["users_page/add_role.jade"]
          template_data: -> options.selected.toJSON()  
        .render().el)


  class ChangePasswordDialog extends FON.Dialog
  
    accept: -> "Change"
    header: -> "Change Password"

    on_accept: (body, options) ->
      password = body.find("#password_input").val()
      repeat_password = body.find("#repeat_password_input").val()
      if (password.length < 5)
        @show_error("Password must be at least 5 characters long")
        return
      if (password != repeat_password)
        @show_error("Passwords don't match")
        return
      user = new User()
      user.id = @model.id
      user.save {password: password},
        success: =>
          @do_hide()
          app.flash
            title: "Password successfully changed."
            hide_after: 2000
        , error: =>
            @do_hide()
            app.flash
              kind: "error"
              title: "Error changing password."

    on_display: (body, options) ->
      body.append(new FON.TemplateController
          tagName: "div"
          template: jade["users_page/change_password.jade"]
          template_data: -> options.selected.toJSON()  
        .render().el)


  app.router.route "/users", "users", ->
    model = new Users
    model.fetch
      success: (model, resp) ->
        app.page new UsersController
          model: model
      error: (model, data, resp) ->
        app.flash
          kind: "error"
          title: "Error: "
          message: "Failed to fetch user information: #{resp}"

  UsersController

