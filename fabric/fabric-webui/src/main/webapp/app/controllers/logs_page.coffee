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
  "controllers/controls/table"
], (app, jade_templ, table) ->

  format_data = (data) ->
    return "" unless data?
    lines = if typeof data == "object"
      if _.isArray(data)
        _.map(data, (x)=> format_data(x))
      else
        for key,value of data
           jade.escape(key) + "=" + format_data(value)
    else
      data.split(/\n/gm)
    lines = for line in lines
      jade.escape(line)
    lines.join("<br>")


  $.fn.dataTableExt.oPagination.bootstrap =
    fnInit: (oSettings, nPaging, fnCallbackDraw) ->
      oSettings.nPaging = $(nPaging)
      oSettings.nPaging.addClass("pagination")

    fnUpdate: (oSettings, fnCallbackDraw) ->
      nPaging = oSettings.nPaging
      nPaging.empty()

      ul = $(document.createElement("ul"))
      nPaging.append(ul)

      page_links = 5
      page_links_mid = Math.floor(page_links / 2)
      total_pages = Math.ceil((oSettings.fnRecordsDisplay()) / oSettings._iDisplayLength)
      current_page = Math.ceil(oSettings._iDisplayStart / oSettings._iDisplayLength) + 1

      if total_pages < page_links
        start_page = 1
        end_page = total_pages
      else
        if current_page <= page_links_mid
          start_page = 1
          end_page = page_links
        else
          if current_page >= (total_pages - page_links_mid)
            start_page = total_pages - page_links + 1
            end_page = total_pages
          else
            start_page = current_page - Math.ceil(page_links / 2) + 1
            end_page = start_page + page_links - 1

      tag = (n)-> $(document.createElement(n))
      add_link = (body, attr)->
        li = tag("li")
        li.attr(attr || {})
        rc = tag("a")
        rc.html(body)
        li.append(rc)
        ul.append(li)
        rc

      add_link("&laquo; "+oSettings.oLanguage.oPaginate.sFirst, {class:"first"}).click ->
        fnCallbackDraw oSettings  if oSettings.oApi._fnPageChange(oSettings, "first")
        false

      add_link("&larr; "+oSettings.oLanguage.oPaginate.sPrevious, {class:"prev"}).click ->
        fnCallbackDraw oSettings  if oSettings.oApi._fnPageChange(oSettings, "previous")
        false

      add_link('...', {class:"disabled"}) unless start_page==1

      i = start_page
      while i <= end_page
        t = ->
          page = i
          attrs = if current_page == page
            {class:"active"}
          else
            {}
          add_link(page, attrs).click ->
            oSettings._iDisplayStart = (page-1) * oSettings._iDisplayLength;
            fnCallbackDraw( oSettings );
            false
        t()
        i++

      add_link('...', {class:"disabled"}) unless end_page==total_pages

      add_link(oSettings.oLanguage.oPaginate.sNext+" &rarr;", {class:"next"}).click ->
        fnCallbackDraw oSettings  if oSettings.oApi._fnPageChange(oSettings, "next")
        false

      add_link(oSettings.oLanguage.oPaginate.sLast+" &raquo;", {class:"last"}).click ->
        fnCallbackDraw oSettings  if oSettings.oApi._fnPageChange(oSettings, "last")
        false

  LogsPage = FON.TemplateController.extend
    template: jade_templ["logs_page/index.jade"]
    template_data: -> {}

    row_render: (oObj) ->
      field = switch oObj.iDataColumn
        when 0 then "host"
        when 1 then "timestamp"
        when 2 then "level"
        when 3 then "message"
        when 4 then "logger"
        when 5 then "properties"

      record = oObj.aData[oObj.iDataColumn]
      data = record[field]

      if( field=="host")
        rc = """<pre class="log"><a href="#/logs/#{format_data(data)}">#{format_data(data)}</a></pre>"""
      else
        rc = """<pre class="log">#{format_data(data)}</pre>"""
      if field == "message" && record.exception
        rc += """<pre class="log">#{format_data(record.exception)}</pre>"""
      rc

    initialize: ->
      FON.TemplateController.prototype.initialize.call(@)
      @key_counter = 0

      get_data_value = (data, sKey) ->
        rc = _.find data, (item)->
          item.name == sKey
        if rc?
          rc.value
        else
          null

      parse_response = (sEcho, data, callback) ->
        output = {}
        output.sEcho = sEcho
        output.iTotalRecords = data.hits.total
        output.iTotalDisplayRecords = data.hits.total
        output.aaData = []
        i = 0
        iLen = data.hits.hits.length

        while i < iLen
          item = []
          source = data.hits.hits[i]._source
          item.push source
          item.push source
          item.push source
          item.push source
          item.push source
          item.push source
          item.push source
          output.aaData.push item
          i++
        callback output

      @request_log = (sSource, data, callback) =>
        columns = [ "host", "timestamp", "level", "logger", "thread", "message", "properties" ]
        sEcho = get_data_value(data, "sEcho")

        level = @log_level.val()
        level_array = [ "error" ]
        unless level == "error"
          level_array.push "warn"
          unless level == "warn"
            level_array.push "info"
            unless level == "info"
              level_array.push "debug"
              level_array.push "trace"  unless level == "debug"

        query_clause = []
        query_clause.push
          terms:
            level:
              level_array

        search_values = @log_search.val()
        if( search_values )
          query_clause.push
            query:
              query_string:
                query: search_values.toLowerCase()

        $.ajax
          url: sSource
          data: JSON.stringify
            from: get_data_value(data, "iDisplayStart")
            size: get_data_value(data, "iDisplayLength")
            sort: [{"timestamp": "desc"}, {"seq": "desc"}]
            query:
              constant_score:
                filter:
                  and: query_clause

          success: (eventData) ->
            json = jQuery.parseJSON(eventData)
            parse_response sEcho, json, callback

          dataType: "text"
          contentType: "application/json"
          processData: false
          type: "POST"
          cache: false
          error: (xhr, error, thrown) ->
            if error == "parsererror"
              alert "warning: JSON data from server could not be parsed. " +
                    "This is caused by a JSON formatting error."

    elements:
      "#log_table": "log_table"
      "#log_search": "log_search"
      "#log_level": "log_level"
      "#pagination": "pagination"
      "form": "form"

    on_render: ->
      @form.submit -> false
      @data_table = @log_table.dataTable
        "sPaginationType": "bootstrap"
        "elPagination": @pagination
        "bPaginate": true,
        "bServerSide": true,
        "sAjaxSource": "log/fon/log/_search",
        "sDom": 'pt',
        "iDisplayLength": 50
        "sScrollX": "100%",
        "fnServerData": @request_log,
        "aaSorting": [[ 1, "desc" ]],
        "aoColumns": [ bSortable: false, bSortable: false, bSortable: false, bSortable: false, bSortable: false, bSortable: false, bSortable: false ]
        aoColumnDefs: [
          fnRender: @row_render
          aTargets: [ 0, 1, 2, 3, 4, 5, 6 ]
        ]

      @log_search.keyup (event)=>
        # use a little delay before submitting so that we dont' pound the server
        # on every keystroke.
        @key_counter += 1
        count = @key_counter
        setTimeout( ()=>
          @data_table.fnDraw @data_table.fnSettings() if @key_counter == count
        , 500);

      @log_level.change (event)=>
        settings = @data_table.fnSettings()
        @data_table.fnDraw settings

  app.router.route "/logs", "logs", ->
    $.ajax
      url: "log.json"
      success: (data) ->
        app.page new LogsPage
      error: (data) ->
        app.flash
          kind: "error"
          title: "Elastic Search is not deployed in the cluster. Log searching facility is not available."

  WindowScroller = ->
    threshold: 25
    at_bottom: ->
      diff = $(document).height() - ( $(window).scrollTop() + $(window).height() )
      diff < @threshold
    scroll_to_bottom: ->
      $(window).scrollTop(9e9)

  ConsolePage = FON.TemplateController.extend
    model: new FON.Model
    collection: new FON.Collection
    template: jade_templ["logs_page/console_page.jade"]
    template_data: -> @model.toJSON()

    initialize: ->
      FON.TemplateController.prototype.initialize.call(@)
      @model.set
        agent: @options.agent
        scrollback: 50
        fields: [ "timestamp", "level", "logger", "thread", "message", "properties"]

    query_clauses: ->

      rc = []
      rc.push
        terms:
          level: switch @log_level.val()
            when "error" then ["error"]
            when "warn" then ["error", "warn"]
            when "info" then ["error", "warn", "info"]
            when "debug" then ["error", "warn", "info", "debug"]
            when "trace" then ["error", "warn", "info", "debug", "trace"]

      rc.push
        term:
          host: @options.agent.toLowerCase()

      search_values = @log_search.val()
      if( search_values )
        rc.push
          query:
            query_string:
              query: search_values.toLowerCase()
      rc

    fetch: ->
      request =
#        fields: @model.get("fields")
        from: 0
        size: @model.get("scrollback")
        sort: [{"timestamp": "desc"}, {"seq": "desc"}],
        query:
          constant_score:
            filter:
              and: @query_clauses()
      @send request, (entries)=>
        @collection.reset(entries.reverse())

    poll: ->
      if @collection.isEmpty()
        @fetch()
      else

        # Only fetch log entries newer than our last entry.
        caluses = @query_clauses()
        caluses.push
          range:
            seq:
              gt: @collection.last().get("seq")

        request =
#          fields: @model.get("fields")
          from: 0
          size: 10000
          sort: [{"timestamp": "asc"}, {"seq": "asc"}],
          query:
            constant_score:
              filter:
                and: caluses

        @send request, (entries)=>
          @collection.add(entries)


    send: (request, success)->
      $.ajax
        url: "log/fon/log/_search"
        dataType: "text"
        contentType: "application/json"
        processData: false
        type: "POST"
        cache: false
        data: JSON.stringify(request)
        success: (data) =>
          at_bottom = @scroller.at_bottom()
          hits = jQuery.parseJSON(data).hits.hits
          entries = for hit in hits
            rc = hit._source
            rc.id = hit._id
            rc
          success(entries)
          if at_bottom
            @scroller.scroll_to_bottom()

        error: (xhr, error, thrown) =>
          if error == "parsererror"
            alert "warning: JSON data from server could not be parsed. " +
                  "This is caused by a JSON formatting error."

    elements:
      "#log_table": "log_table"
      "#log_search": "log_search"
      "#log_level": "log_level"
      "form": "form"

    on_render: ->
      @scroller = new WindowScroller()
      @form.submit -> false
      @log_search.keyup _.throttle((event)=>
        console.log "updaing"
        @fetch()
      , 500);

      @log_level.change (event)=>
        @fetch()

      @table = new table
        el: @log_table
        collection: @collection
        row_template: jade_templ["logs_page/log_row.jade"]
        row_template_data: (model)->
          rc = model.toJSON()
          rc.format_data = format_data
          rc
      @table.render()

      @fetch()

  app.router.route "/logs/:agent", "logs", (agent)->
    $.ajax
      url: "log.json"
      success: (data) ->
        app.page new ConsolePage
          agent:agent
      error: (data) ->
        app.flash
          kind: "error"
          title: "Elastic Search is not deployed in the cluster. Log searching facility is not available."

  LogsPage