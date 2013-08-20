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
  "controllers/agents_page"
  "models/compute_services"
  "controllers/controls/collection"
  "controllers/controls/validating_text_input"
], (app, jade, AgentsController, ComputeServices) ->


  class WizardPage extends FON.TemplateController
    tagName: "fieldset"
    validated_controls = []

    initialize: ->
      super
      @container = @options.container if @options.container
      @prev = @options.prev if @options.prev
      @state = @options.state if @options.state
      @next = @get_next()

    get_next: -> null

    sync_state: ->

    maybe_enable_next: (self) ->
      valid = true
      keep_checking = true
      if self.validated_controls
        for control in self.validated_controls
          if !control.validate() && valid
            valid = false

      if valid
        if self.next
          self.container.enable_next "Next", -> self.on_next()
        else
          self.container.enable_next "Finish", -> self.on_finish()
      else
        self.container.disable_next()

    on_back: ->
      if (@prev)
        @sync_state()
        @container.set_page(@prev)
      false

    on_next: ->
      if (@next)
        @sync_state()
        @container.set_page(@next)
      false

    on_finish: ->
      @sync_state()
      false

    on_render: ->
      if (@prev)
        @container.enable_back => @on_back()
      else
        @container.disable_back()

      if (@next)
        @container.configure_next(
          "Next"
          => @on_next()
        )
      else
        @container.configure_next(
          "Finish"
          => @on_finish()
        )


  class RootAgentFilteredList extends FON.CollectionController
    on_add: (agent) ->
      if (agent.get("root") && agent.get("alive"))
        super(agent)


  class RootAgentSelectControl extends FON.TemplateController
    tagName: "li"
    className: "padded"
    template: jade["agents_page/create_agent_wizard/agent_selector_entry.jade"]
    template_data: -> @model.toJSON()
    elements:
      "input": "input"

    initialize: ->
      super
      @state = @options.state if @options.state

    on_render: (self) =>
      @input.click (event) =>
        @state.set {selected: @model}
      if (@state.get("selected") == @model)
        @input.prop("checked", true)


  class AddChildAgentPage1 extends WizardPage
    template: -> jade["agents_page/create_agent_wizard/add_child_agent_page_1.jade"]
    elements:
      "ul.inputs-list.agent-selector": "agent_selector"

    on_finish: ->
      @sync_state()
      arguments =
        providerType: "child"
        name: @state.get "name"
        parent: @state.get("selected").id
        number: @state.get("number")
        proxyUri: @state.get("proxy")
        version: @state.get("version").id
        profiles: @state.get("profiles").join(", ")
        ensembleServer: @state.get("ensemble_server")
        jvmOpts: @state.get("jvmopts")
        resolver: @state.get "resolver"

      options =
        success: (data, textStatus, jqXHR) =>

        error: (model, response, options) =>
          app.flash
            kind: "error"
            title: "Server Error : "
            message: "Container creation failed due to #{response.statusText} : \u201c#{response.responseText}\u201d"

      @model.create arguments, options
      @container.do_return()
      false

    on_render: (self) ->
      super

      ul = new RootAgentFilteredList
        el: @agent_selector
        collection: @model
        child_control: (agent) =>
          new RootAgentSelectControl
            model: agent
            state: @state

      ul.render()

      if (!@state.get("selected"))
        $(ul.el).find("input[name=root-agent]:first").click()


  class AddSSHAgentPage1 extends WizardPage
    template: -> jade["agents_page/create_agent_wizard/add_ssh_agent_page_1.jade"]
    elements:
      ":input[name='hostname']": "hostname"
      ":input[name='port']": "port"
      ":input[name='user']": "username"
      ":input[name='password']": "password"
      "input[name=use-pk-file]": "use_pk_file"
      "input[name=pk-file]": "pk_file"
      "a.browse": "browse"

    initialize: ->
      super
      @state.set {port: 22}

    get_next: ->
      new AddSSHAgentPage2
        model: @model
        container: @container
        prev: @
        state: @state

    sync_state: ->
      pk_file = null

      pk_file = @pk_file.val() if @use_pk_file.is ":checked"

      @state.set {
        hostname: @hostname.val()
        port: @port.val()
        username: @username.val()
        password: @password.val()
        pk_file: pk_file
      }

    on_render: ->
      super

      @validated_controls = []
      @validated_controls.push new FON.ValidatingTextInput
        control: @hostname
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false,
            msg: "You must specify a valid hostname"
          else
            ok: true,
            msg: ""
        cb: @maybe_enable_next

      @validated_controls.push new FON.ValidatingTextInput
        control: @port
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false,
            msg: "You must specify a valid port number"
          else if isNaN(text)
            ok: false,
            msg: "You must specify a valid port number"
          else
            ok: true,
            msg: ""
        cb: @maybe_enable_next

      @validated_controls.push new FON.ValidatingTextInput
        control: @username
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false,
            msg: "You must specify a username"
          else
            ok: true,
            msg: ""
        cb: @maybe_enable_next

      @validated_controls.push new FON.ValidatingTextInput
        control: @password
        controller: @
        validator: (text) =>
          if !@use_pk_file.is(":checked") && (!text || text == "")
            ok: false,
            msg: "You must specify a password"
          else
            ok: true,
            msg: ""
        cb: @maybe_enable_next

      @hostname.bind "DOMNodeInsertedIntoDocument", =>
        @hostname.focus()
        @maybe_enable_next(@)

      @use_pk_file.change (event) =>
        if @use_pk_file.is ":checked"
          @pk_file.prop "disabled", false
        else
          @pk_file.prop "disabled", true
          @pk_file.val ""
        @maybe_enable_next(@)

      if @state.get "pk_file"
        @use_pk_file.attr "checked", true
        @pk_file.val @state.get "pk_file"
        @pk_file.prop "disabled", false
      else
        @use_pk_file.attr "checked", false
        @pk_file.prop "disabled", true

      @hostname.val @state.get "hostname"
      @port.val @state.get "port"
      @username.val @state.get "username"
      @password.val @state.get "password"


  class AddSSHAgentPage2 extends WizardPage
    template: -> jade["agents_page/create_agent_wizard/add_ssh_agent_page_2.jade"]

    elements:
      ":input[name='path']": "path"
      ":input[name='retries']": "retries"
      ":input[name='retry-delay']": "retry_delay"

    initialize: ->
      super
      @state.set
        retries: 1
        retry_delay: 1

    sync_state: ->
      @state.set
        path: @path.val()
        retries: @retries.val()
        retry_delay: @retry_delay.val()

    on_finish: ->
      @sync_state()
      arguments =
        providerType: "ssh"
        name: @state.get "name"
        host: @state.get "hostname"
        username: @state.get "username"
        #password: @state.get "password"
        port: @state.get "port"
        path: @state.get "path"
        sshRetries: @state.get "retries"
        retryDelay: @state.get "retry_delay"
        number: @state.get "number"
        proxyUri: @state.get "proxy"
        version: @state.get("version").id
        profiles: @state.get("profiles").join(", ")
        ensembleServer: @state.get "ensemble_server"
        jvmOpts: @state.get "jvmopts"
        resolver: @state.get "resolver"

      if @state.get "pk_file"
        arguments['privateKeyFile'] = @state.get "pk_file"
        arguments['passPhrase'] = @state.get "password"
      else
        arguments['password'] = @state.get "password"

      options =
        success: (data, textStatus, jqXHR) =>

        error: (jqXHR, textStatus, errorThrown) =>
          app.flash
            kind: "error"
            title: "Server Error : "
            message: "Container creation failed due to #{textStatus.statusText} : \u201c#{textStatus.responseText}\u201d"

      @model.create arguments, options
      @container.do_return()
      false

    on_render: ->
      super

      @validated_controls = []
      @validated_controls.push new FON.ValidatingTextInput
        control: @path
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false,
            msg: "You must specify a path"
          else
            ok: true,
            msg: ""
        cb: @maybe_enable_next

      @validated_controls.push new FON.ValidatingTextInput
        control: @retries
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false
            msg: "You must specify a valid retry count"
          else if isNaN(text)
            ok: false
            msg: "You must specify a valid retry count"
          else
            ok: true
            msg: ""
        cb: @maybe_enable_next

        @validated_controls.push new FON.ValidatingTextInput
          control: @retry_delay
          controller: @
          validator: (text) ->
            if !text || text == ""
              ok: false
              msg: "You must specify a valid retry count"
            else if isNaN(text)
              ok: false
              msg: "You must specify a valid retry count"
            else
              ok: true,
              msg: ""
          cb: @maybe_enable_next

        if !@state.get "path"
          @state.set
            path: "/home/#{@state.get("username")}"

        @path.val @state.get "path"
        @retries.val @state.get "retries"
        @retry_delay.val @state.get "retry_delay"


  class AddJCloudsAgentPage1 extends WizardPage
    template: -> jade["agents_page/create_agent_wizard/add_cloud_agent_page_1.jade"]

    elements:
      "select[name=provider]": "provider"
      ":input[name=instance-type]": "instance_type"
      ":input[name=type-selection]": "type_selection"
      "input[name=remember]": "remember_credentials"
      ":input[name=hardware-id]": "hardware_id"
      "#hardware-id": "hardware_id_div"

    initialize: ->
      super
      @compute_services = @options.compute_services

    get_next: -> true

    on_next: ->
      type_selection = $(@type_selection).filter(":checked").val()

      if type_selection == "by-os"
        if !@by_os_pages
          @by_os_pages = new AddJCloudsAgentByOSPage
            model: @model
            container: @container
            prev: @
            state: @state
        @next = @by_os_pages
      else if type_selection == "by-image"
        if !@by_image_pages
          @by_image_pages = new AddJCloudsAgentByImagePage
            model: @model
            container: @container
            prev: @
            state: @state
        @next = @by_image_pages

      super

    sync_state: ->
      @state.set
        provider: @provider.val()
        instance_type: @instance_type.val()
        hardware_id: @hardware_id.val()
        type_selection: $(@type_selection).filter(":checked").val()

    maybe_show_custom: (event) ->
      if @instance_type.val() == "Custom"
        @hardware_id_div.removeClass "hide"
      else
        @hardware_id_div.addClass "hide"

    on_render: ->
      super

      @compute_services.each (service) =>
        @provider.append $("<option/>",
          value: service.id
          text: service.get "name")

      @provider.bind "DOMNodeInsertedIntoDocument", =>
        @provider.focus()
        @maybe_enable_next(@)

      @instance_type.bind "DOMNodeInsertedIntoDocument", (event) => @maybe_show_custom(event)
      @instance_type.change (event) => @maybe_show_custom(event)

      if !@state.get "type_selection"
        @state.set
          type_selection: "by-os"

      selection = @state.get "type_selection"
      for element in @type_selection
        el = $(element)
        el.attr "checked", (el.val() == selection)

      @provider.val @state.get "provider"
      @instance_type.val @state.get "instance_type"
      @hardware_id.val @state.get "hardware_id"

    poll: ->
      @compute_services.fetch({op: "update"}) if @compute_services


  class AddJCloudsAgentByOSPage extends WizardPage
    template: -> jade["agents_page/create_agent_wizard/add_cloud_agent_by_os_page.jade"]

    elements:
      "select[name=os-family]": "os_family"
      "select[name=os-version]": "os_version"

    mask:
      suse: "SUSE"
      debian: "Debian"
      centos: "CentOS"
      rhel: "Red Hat Enterprise Linux"
      solaris: "Solaris"
      ubuntu: "Ubuntu"
      windows: "Windows"

    initialize: ->
      super
      @os_and_versions_map = app.cloud_os_and_versions.toJSON()

    get_next: ->
      new AddJCloudsAgentPage3
        model: @model
        container: @container
        prev: @
        state: @state

    sync_state: ->
      @state.set
        os_family: @os_family.val()
        os_version: @os_version.val()

    update_os_version: ->
      @os_version.empty()
      selected = @os_family.val()
      versions = @os_and_versions_map[selected]
      for key, value of versions
        @os_version.append """<option value="#{value}">#{key}</option>"""

    on_render: ->
      super

      for os of @os_and_versions_map
        @os_family.append """<option value="#{os}">#{@mask[os]}</option>"""

      @os_family.change => @update_os_version()

      if @state.get "os_family"
        @os_family.val @state.get "os_family"

      @update_os_version()

      if @state.get "os_version"
        @os_version.val @state.get "os_version"


  class AddJCloudsAgentByImagePage extends WizardPage
    template: -> jade["agents_page/create_agent_wizard/add_cloud_agent_by_image_page.jade"]

    elements:
      ":input[name=image-id]": "image_id"
      ":input[name=location-id]": "location_id"

    get_next: ->
      new AddJCloudsAgentPage3
        model: @model
        container: @container
        prev: @
        state: @state

    sync_state: ->
      @state.set
        image_id: @image_id.val()
        location_id: @location_id.val()

    on_render: ->
      super

      @validated_controls = []

      @validated_controls.push new FON.ValidatingTextInput
        control: @image_id
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false,
            msg: "You must specify what image ID to use for the new nodes"
          else
            ok: true,
            msg: ""
        cb: @maybe_enable_next

      if @state.get("provider") == "aws-ec2"
        @validated_controls.push new FON.ValidatingTextInput
          control: @location_id
          controller: @
          validator: (text) ->
            if !text || text == ""
              ok: false,
              msg: "You must specify what location to use for the new nodes"
            else
              ok: true,
              msg: ""
          cb: @maybe_enable_next

      @image_id.bind "DOMNodeInsertedIntoDocument", =>
        @image_id.focus()
        @maybe_enable_next(@)

      @image_id.val @state.get "image_id"
      @location_id.val @state.get "location_id"


  class AddJCloudsAgentPage3 extends WizardPage
    template: -> jade["agents_page/create_agent_wizard/add_cloud_agent_page_3.jade"]

    elements:
      ":input[name=user-account]": "user_account"
      ":input[name=owner]": "owner"
      ":input[name=group]": "group"
      ":input[name=disable-admin-access]": "disable_admin_access"

    sync_state: ->
      @state.set
        user_account: @user_account.val()
        owner: @owner.val()
        group: @group.val()
        adminAccess: !@disable_admin_access.is(":checked")


    on_finish: ->
      @sync_state()
      arguments =
        providerType: "jclouds"
        name: @state.get "name"
        providerName: @state.get "provider"
        #identity: @state.get "identity"
        #credential: @state.get "credential"
        user: @state.get "user_account"
        group: @state.get "group"
        owner: @state.get "owner"
        number: @state.get "number"
        proxyUri: @state.get "proxy"
        version: @state.get("version").id
        profiles: @state.get("profiles").join(", ")
        adminAccess: @state.get "adminAccess"
        ensembleServer: @state.get "ensemble_server"
        jvmOpts: @state.get "jvmopts"
        resolver: @state.get "resolver"

      arguments.group = "fabric" if !@state.get("group") || @state.get("group") == ""
      arguments.owner = null if !@state.get("owner") || @state.get("owner") == ""
      arguments.user = null if !@state.get("user_account") || @state.get("user") == ""

      if @state.get("instance_type") == "Custom"
        arguments['hardwareId'] = @state.get "hardware_id"
      else
        arguments['instanceType'] = @state.get "instance_type"

      if @state.get("type_selection") == "by-os"
        arguments['osFamily'] = @state.get "os_family"
        arguments['osVersion'] = @state.get "os_version"
      else
        arguments['imageId'] = @state.get "image_id"
        arguments['locationId'] = @state.get "location_id"


      options =
        success: (data, textStatus, jqXHR) =>

        error: (jqXHR, textStatus, errorThrown) =>
          app.flash
            kind: "error"
            title: "Server Error : "
            message: "Container creation failed due to : \u201c#{textStatus.responseText}\u201d"

      @model.create arguments, options
      @container.do_return()
      false

    on_render: ->
      super

      @validated_controls = []

      @user_account.bind "DOMNodeInsertedIntoDocument", =>
        @user_account.focus()
        @maybe_enable_next(@)

      @user_account.val @state.get "user_account"
      @owner.val @state.get "owner"
      @group.val @state.get "group"


  class AgentTypeSelectionPage extends WizardPage
    template: -> jade["agents_page/create_agent_wizard/create_agent_type_selection.jade"]
    elements:
      "li.clouds": "cloud_option"

    initialize: ->
      super
      @compute_services = new ComputeServices
      @compute_services.fetch()

      @compute_services.bind "change", @render, @

    get_next: -> true

    on_next: ->
      @selection = $("input[name=agent-type]:checked").val()

      if (@selection == "child")
        if (!@child_agent_pages)
          @child_agent_pages = new AddChildAgentPage1
            model: @model
            container: @container
            prev: @
            state: @state
        @next = @child_agent_pages
      else if (@selection == "ssh")
        if (!@ssh_agent_pages)
          @ssh_agent_pages = new AddSSHAgentPage1
            model: @model
            container: @container
            prev: @
            state: @state
        @next = @ssh_agent_pages
      else if (@selection == "jclouds")
        if (!@jclouds_agent_pages)
          @jclouds_agent_pages = new AddJCloudsAgentPage1
            model: @model
            container: @container
            prev: @
            state: @state
            compute_services: @compute_services
        @next = @jclouds_agent_pages

      super

    on_render: (self) ->
      super

      if @compute_services.length == 0
        @cloud_option.css "display", "none"

      el = $(@el)

      if (@selection)
        radio = el.find("input[name=agent-type]").filter("[value=#{@selection}]")
        radio.prop("checked", true)
      else
        radio = el.find("input[name=agent-type]").filter("[value=child]")
        radio.prop("checked", true)

    poll: ->
      @compute_services.fetch
        op: "update"


  class AgentProfileConfigPage extends WizardPage
    template: jade["agents_page/create_agent_wizard/agent_profile_config.jade"]
    template_data: ->
      versions: app.versions.models
      profiles: @profiles

    initialize: ->
      super
      @state.set {version: app.versions.default_version()}

    elements:
      "#version": "version_select"
      ".inputs-list": "profile_list"

    get_next: ->
      new AgentTypeSelectionPage
        model: @model
        container: @container
        prev: @
        state: @state

    sync_state: ->
      checked = $("input:checked")
      profile_names = []
      for profile in checked
        profile_names.push profile.name
      @state.set {profiles: profile_names}

    update_profile_list: ->
      @profile_list.empty()

      version = @state.get "version"
      profile_names = @state.get "profiles"

      all_profiles = version.get "_profiles"
      all_profiles = _.difference all_profiles, version.get "abstract_profiles"

      for profile in all_profiles
        profile = profile.trim()
        li = _.template("<li><div class=\"clearfix\" style=\"margin: 0px;\"><label><input type=\"checkbox\" name=\"#{profile}\" value=\"#{profile}\"><span>#{profile}</span></label></div></li>")

        if profile_names
          for p in profile_names
            if profile == p
              $(":input", li).prop("checked", true)

        @profile_list.append(li)

    on_render: ->
      super
      @version_select.val(@state.get("version").attributes.id)

      @version_select.change (event) =>
        for version in app.versions.models
          if version.id == $("select option:selected").val()
            @state.set {version: version}
            @state.unset "profiles"
        @update_profile_list()

      @update_profile_list()


  class AgentConfigurationPage extends WizardPage
    template: jade["agents_page/create_agent_wizard/standard_agent_details_page.jade"]

    elements:
        ":input[name=name]": "name"
        ":input[name=ensemble-server]": "ensemble_server"
        ":input[name=use-jvm-opts]": "use_jvm_opts"
        ":input[name=jvm-opts]": "jvm_opts"
        ":input[name=proxy]": "proxy"
        ":input[name=use-proxy]": "use_proxy"
        ":input[name=number]": "number"
        "select[name=resolver]": "resolver"


    initialize: ->
      super
      @state.set {number: 1}

    get_next: ->
      new AgentProfileConfigPage
        model: @model
        container: @container
        prev: @
        state: @state

    sync_state: ->
      proxy = null
      jvmopts = null
      proxy = @proxy.val() if @use_proxy.is ":checked"
      jvmopts = @jvm_opts.val() if @use_jvm_opts.is ":checked"
      ensemble_server = @ensemble_server.is ":checked"

      @state.set {
        name: @name.val()
        ensemble_server: ensemble_server
        jvmopts: jvmopts
        proxy: proxy
        number: @number.val()
        resolver: @resolver.val()
      }

    on_render: ->
      super

      @validated_controls = []

      @validated_controls.push new FON.ValidatingTextInput
        control: @name
        controller: @
        validator: (text) ->
          regex = /^[a-zA-Z0-9_-]*$/
          if !text || text == ""
            ok: false,
            msg: "You must specify a container name"
          else if !regex.test(text)
            ok: false
            msg: "Name can only contain letters, numbers, \u201c-\u201d, and \u201c_\u201d"
          else
            ok: true,
            msg: ""
        cb: @maybe_enable_next

      @validated_controls.push new FON.ValidatingTextInput
        control: @number
        controller: @
        validator: (text) ->
          if !text || text == ""
            ok: false,
            msg: "You must specify how many containers to create"
          else if isNaN(text)
            ok: false,
            msg: "Count must be a positive integer"
          else
            ok: true,
            msg: "If greater than 1, multiple containers will be created with instance numbers appended to the name"
        cb: @maybe_enable_next

      @use_proxy.click (event) =>
        if @use_proxy.is ":checked"
          @proxy.prop "disabled", false
        else
          @proxy.prop "disabled", true
          @proxy.val("")
        true

      @use_jvm_opts.click (event) =>
        if @use_jvm_opts.is ":checked"
          @jvm_opts.prop "disabled", false
        else
          @jvm_opts.prop "disabled", true
          @jvm_opts.val ""

      @name.bind "DOMNodeInsertedIntoDocument", =>
        @name.focus()
        @maybe_enable_next(@)

      @ensemble_server.prop "checked", @state.get "ensemble_server"

      if @state.get "jvmopts"
        @use_jvm_opts.attr("checked", true)
        @jvm_opts.val @state.get "jvmopts"
        @jvm_opts.prop "disabled", false
      else
        @use_jvm_opts.attr "checked", false
        @jvm_opts.prop "disabled", true

      if @state.get "proxy"
        @use_proxy.attr("checked", true)
        @proxy.val @state.get "proxy"
        @proxy.prop "disabled", false
      else
        @use_proxy.attr("checked", false)
        @proxy.prop "disabled", true

      @number.val @state.get "number"
      @name.val @state.get "name"
      @resolver.val @state.get "resolver"


  class WizardIntro extends WizardPage
    template: jade["agents_page/create_agent_wizard/create_agent_intro_page.jade"]

    get_next: ->
      new AgentConfigurationPage
        model: @model
        container: @container
        prev: @
        state: @state


  class AddAgentWizard extends FON.TemplateController
    template: jade["common/wizard.jade"]
    template_data: ->
      header: "Create Container"
    elements:
      ".body": "body"
      "a.next": "next"
      "a.back": "back"
      "a.cancel": "cancel"

    initialize: ->
      super

      @state = new FON.Model

      @page1 = new AgentConfigurationPage
        model: @model
        container: @
        state: @state

      @state.bind "change:page", @refresh_content, @

      @do_return = @options.do_return if @options.do_return

    on_render: (self) ->

      @cancel.click (event) =>
        @do_return()
        false

      if (!@state.get("page"))
        self.set_page(@page1)

    do_return: ->

    disable_next: ->
      @next.unbind('click')
      @next.click (event) -> false
      @next.addClass("disabled")

    enable_next: (text, handler) -> @configure_next(text, handler)

    configure_next: (text, handler) ->
      @next.unbind('click')
      @next.removeClass("disabled")
      @next.text(text)
      if (handler)
        @next.click (event) -> handler(event)
      else
        @next.click (event) -> false

    enable_back: (handler) ->
      @back.unbind('click')
      @back.removeClass("disabled")
      if (handler)
        @back.click (event) -> handler(event)
      else
        @back.click (event) -> false

    disable_back: ->
      @back.unbind('click')
      @back.click (event) -> false
      @back.addClass("disabled")

    clear_handlers: ->
      @back.unbind('click')
      @next.unbind('click')
      @back.click (event) -> false
      @next.click (event) -> false

    set_page: (page) -> @state.set {page: page}

    refresh_content: ->
      page = @state.get("page")
      @body.empty()
      @body.append(page.render().el)

    poll: ->
      @model.fetch
        op: "update"
      page = @state.get "page"
      if page && page.poll && _.isFunction(page.poll)
        page.poll()

  AddAgentWizard

