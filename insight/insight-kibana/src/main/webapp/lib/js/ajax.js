$(document).ready(function () {

  // Bind all click/change/whatever handlers
  bind_clicks()
  popover_setup()

  // Common color profile.
  window.graph_colors = ["#edc240", "#afd8f8", "#cb4b4b", "#4da74d", "#9440ed"]

  // Hide sidebar by default
  sbctl('hide',false);

  // Handle AJAX errors
  $("div#logs").ajaxError(function (e, xhr, settings, exception) {
    $('#meta').text("");
    if(xhr.statusText != 'abort') {
      showError("<strong>Oops!</strong> Something went terribly wrong.",
        "I'm not totally sure what happened, but maybe refreshing, or "+
        "hitting Reset will help. If that doesn't work, you can try "+
        "restarting your browser. If all else fails, it is possible your"+
        " configuation has something funky going on. <br><br>If it helps,"+
        " I received a <strong>" + xhr.status + " " + xhr.statusText +
        "</strong> from: " + settings.url);
    }
  });

  // Whenever the URL changes, fire this.
  $.history.init(pageload);

  // Resize flot graph with window
  $(window).smartresize(function () {
    if ($(".legend").length > 0){
      logGraph(window.graphdata,window.interval,window.hashjson.graphmode);
    }
  });
});

// This gets called every time the URL changes,
// Including hash changes, setHash() will
// cause a reload of the results
function pageload(hash) {
  if (typeof window.request !== 'undefined') {
    window.request.abort();
    window.segment = undefined;
    try { delete window.segment; } catch (e) {}
  }
  //if hash value exists, run the ajax
  if (hash) {
    window.hashjson = JSON.parse(Base64.decode(hash));

    //console.log(window.hashjson)

    // Take the hash data and populate the search fields
    $('#queryinput').val(window.hashjson.search);
    $('#timeinput').val(window.hashjson.timeframe);

    // Figure out defaults. Need more here
    if(typeof window.hashjson.graphmode == 'undefined')
      window.hashjson.graphmode = 'count';

    if(typeof window.hashjson.time == 'undefined')
      window.hashjson.time = {user_interval:0};
    else if (typeof window.hashjson.time.user_interval == 'undefined')
        window.hashjson.time.user_interval = 0;

    switch (window.hashjson.mode) {
    case 'terms':
    case 'score':
    case 'trend':
    case 'mean':
      getAnalysis();
      break;
    case 'id':
      getID();
      break;
    default:
      $('#feedlinks').html(feedLinks(window.hashjson));
      getPage();
      break;
    }

  } else {
    resetAll();
  }
}

function getPage() {

  if (window.inprogress) {
    return false;
  }
  window.inprogress = true;

  // Show the user an animated loading thingy
  setMeta('loading');

  var sendhash = encodeURIComponent(window.location.hash.replace(/^#/, ''));

  //Get the data and display it
  window.request = $.ajax({
    url: "api/search/"+sendhash,
    type: "GET",
    cache: false,
    success: function (json) {
      // Make sure we're still on the same page
      if (sendhash == encodeURIComponent(window.location.hash.replace(/^#/, ''))) {
        //Parse out the result
        window.resultjson = JSON.parse(json);

        //console.log(
        //  'curl -XGET \'http://localhost:9200/'+resultjson.index+
        //  '/_search?pretty=true\' -d\''+resultjson.kibana.es_query+'\'');
        //console.log(resultjson.kibana.curl_call);

        $('#graphheader,#graph').text("");
        $('#feedlinks').html(feedLinks(window.hashjson));

        // Make sure we get some results before doing anything
        if ((typeof window.resultjson.kibana.error !== 'undefined') || (!(resultjson.hits))) {
          setMeta(0);
          showError('No events matched',"Sorry, I couldn't find anything for " +
            "that query. Double check your spelling and syntax.");
          return;
        }

        if (!(resultjson.hits.total > 0)) {
          setMeta(0);
          showError('No events matched',"Sorry, I couldn't find anything for " +
            "that query. Double check your spelling and syntax.");
          return;
        }

        // Determine fields to be displayed
        var fields = window.hashjson.fields.length == 0 ?
          resultjson.kibana.default_fields : window.hashjson.fields

        // Create 'Columns' section
        $('#fields').html("<div class='input-prepend'>" +
          "<span class='add-on'><i class='icon-columns'></i></span>" +
          "<input id='field_filter' type='text' class='span' placeholder='Columns' /></div>" +
          "<ul class='unselected nav nav-pills nav-stacked'></ul>");

        var all_fields = array_unique(get_all_fields(resultjson).concat(fields))

        // Create sidebar field list
        var fieldstr = '';
        for (var index in all_fields) {
          var field_name = all_fields[index].toString();
          var afield = field_alias(field_name) + "_field";
          var mode = $.inArray(field_name,window.hashjson.fields) >= 0 ?
            'selected' : 'unselected'
          fieldstr += sidebar_field_string(field_name,mode);
        }
        $('#fields ul.unselected').append(fieldstr)

        // Store list of fields for filter use
        window.field_list = $('#fields > ul > li');

        //var fieldstr = '';
        //for (var index in window.hashjson.fields) {
        //  var field_name = window.hashjson.fields[index].toString();
        //  var afield = field_alias(field_name) + "_field";
        //  $('#fields ul.unselected li.' + afield).hide();
        //  fieldstr += sidebar_field_string(field_name,'minus');
        //}
        //$('#fields ul.selected').append(fieldstr)

        enable_popovers();

        // Create and populate #logs table
        $('#logs').html(CreateLogTable(
          window.resultjson.hits.hits, fields,
          'table logs table-condensed'
        ));

        pageLinks();

        // Populate hit and total
        setMeta(window.resultjson.hits.total);

        window.interval = calculate_interval(
          Date.parse(window.resultjson.kibana.time.from),
          Date.parse(window.resultjson.kibana.time.to),
          100,
          window.hashjson.time.user_interval
        )

        if (typeof window.sb == 'undefined') {
          sbctl('show',false);
        } else {
          sbctl(window.sb,false);
        }

        // Create and populate graph
        $('#graph').html(
          '<center><br><p><img src=images/barload.gif></center>');
        getGraph(window.interval);

      }
    }
  });
  window.inprogress = false;
}

function getGraph(interval) {

  //generate the parameter for the php script
  var sendhash = encodeURIComponent(window.location.hash.replace(/^#/, ''));
  var mode = window.hashjson.graphmode;
  window.segment = typeof window.segment === 'undefined' ? '' : window.segment;
  //Get the data and display it
  window.request = $.ajax({
    url: "api/graph/"+mode+"/"+interval+"/"+sendhash+"/"+window.segment,
    type: "GET",
    cache: false,
    success: function (json) {
      // Make sure we're still on the same page
      if (sendhash == encodeURIComponent(window.location.hash.replace(/^#/, ''))) {

        //Parse out the returned JSON
        var graphjson = JSON.parse(json);

        if ($(".legend").length > 0) {
          window.graphdata = graphjson.facets[mode].entries.concat(
            window.graphdata);
          window.graphhits = graphjson.hits.total + window.graphhits;
        } else {
          window.graphdata = graphjson.facets[mode].entries;
          window.graphhits = graphjson.hits.total;
        }

        setMeta(window.graphhits);

        // Display graph data
        logGraph(window.graphdata, interval, mode);

        if (typeof graphjson.kibana.next !== 'undefined') {
          window.segment = graphjson.kibana.next;
          if (!($(".graphloading").length > 0)) {
            $('div.legend table, div.legend table td').css({
              "background-image": "url(images/barload.gif)",
              "background-size":  "100% 100%"
            });
          }
          getGraph(interval);
        } else {
          if(typeof window.segment !== 'undefined')
            window['segment'] = undefined;
            try { delete window['segment'] } catch (e) {}
        }

      }
    }
  });
}

// create a pie chart for a terms facet
function pieChart(data,selector){

  var plot = $.plot($(selector), data, {
    series: {
      pie: {
        radius: 1,
        show: true,
        combine: {
          color: '#999',
          label: 'The Rest'
        },
        label: { show: false }
      }
    },
    grid: { hoverable: true, clickable: true },
    legend: { show: false }
  });

  // This should be generalized. Too much copy + paste
  var previousLabel = null;
  $(selector).bind("plothover", function (event, pos, item) {
    if (item) {
      if (previousLabel != item.series.label) {
        previousLabel = item.series.label;
        $("#tooltip").remove();
        var label = (!item.series.label) ? "missing" : item.series.label;
        showTooltip(
          pos.pageX+30, pos.pageY, 
          "<b>"+label+"</b>" + " " + Math.round(item.series.percent) + "%"
        );
      }
    } else {
      $("#tooltip").remove();
      previousLabel = null;
    }
  });

}

function analyzeField(field, mode) {
  window.hashjson.mode = mode;
  window.hashjson.analyze_field = field;
  setHash(window.hashjson);
}

function getID() {
  // Show the user an animated loading thingy
  var sendhash = encodeURIComponent(window.location.hash.replace(/^#/, ''));
  //Get the data and display it
  window.request = $.ajax({
    url: "api/id/"+window.hashjson.id+"/"+window.hashjson.index,
    type: "GET",
    cache: false,
    success: function (json) {
      window.resultjson = JSON.parse(json)
      var hit = resultjson.hits.hits[0]
      blank_page();
      setMeta(1);

      var str = details_table(0, 'table table-bordered');

      $('#graph').html(
        "<h2>Details for log ID: "+hit._id+" in "+hit._index+"</h2><br>"+str);
    }
  });
  sbctl('hide',false)
  window.hashjson.id = undefined;
  window.hashjson.index = undefined
  window.hashjson.mode = undefined
  try{
    delete window.hashjson.id;
    delete window.hashjson.index
    delete window.hashjson.mode
  }catch(e){}
}

function getAnalysis() {
  setMeta('loading');
  //generate the parameter for the php script
  var sendhash = encodeURIComponent(window.location.hash.replace(/^#/, ''));
  //Get the data and display it
  window.request = $.ajax({
    url: "api/analyze/" + window.hashjson.analyze_field + "/" +
      window.hashjson.mode + "/" + sendhash,
    type: "GET",
    cache: false,
    success: function (json) {
      // Make sure we're still on the same page
      if (sendhash == encodeURIComponent(window.location.hash.replace(/^#/, ''))) {

        //Parse out the returned JSON
        var field = window.hashjson.analyze_field;
        resultjson = JSON.parse(json);

        $('.pagelinks').html('');
        $('#fields').html('');

        if(typeof resultjson.error !== 'undefined') {
          setMeta(0);
          showError('Statistical analysis unavailable for '+field +
            ' <button class="btn tiny btn-info" ' +
            'style="display: inline-block" id="back_to_logs">back to logs' +
            '</button>',
            "I'm not able to analyze <strong>" + field + "</strong>. " +
            "Statistical analysis is only available for fields " +
            "that are stored a number (eg float, int) in ElasticSearch");
          return;
        }

        window.interval = calculate_interval(
          Date.parse(window.resultjson.kibana.time.from),
          Date.parse(window.resultjson.kibana.time.to),
          100,
          window.hashjson.time.user_interval
        )

        if(resultjson.hits.total == 0) {
          setMeta(resultjson.hits.total);
          showError('No events matched '+
            '<button class="btn tiny btn-info" ' +
            'style="display: inline-block" id="back_to_logs">back to logs' +
            '</button>',
            "Sorry, I couldn't find anything for " +
            "that query. Double check your spelling and syntax.");
          return;
        }

        setMeta(resultjson.hits.total);
        var analyze_field = window.hashjson.analyze_field.split(',,').join(' ');

        switch (window.hashjson.mode) {
        case 'terms':
          var index_count  = $.isArray(resultjson.kibana.index) ?
            resultjson.kibana.index : resultjson.kibana.index.split(',').length;

          // add the missing count for the terms table and pie chart
          // This should really insert at the right point.
          resultjson.facets.terms.terms.push({
            term: '',
            count: resultjson.facets.terms.missing
          });

          var title = ''+
            '<h2>Terms Facet of ' +
            '<strong>' + analyze_field + '</strong> field(s) ' +
            '<button class="btn tiny btn-info" ' +
            'style="display: inline-block" id="back_to_logs">back to logs' +
            '</button>' +
            '<div class=pull-left id="piechart"></div></h2>' +
            'This analysis is based on the events in the <strong>' +
            index_count +'</strong> most recent indices ' +
            'for your query in your selected timeframe.<br><br>'+
            '';

          $('#logs').html(
            title+CreateTableView(termsTable(resultjson),'logs analysis'));

          sbctl('hide',false)
          graphLoading();
          window.hashjson.graphmode = 'count'
          getGraph(window.interval);

          // Calculate data for pie chart
          var data = [];
          $.each(resultjson.facets.terms.terms,function(i,term) {
            data[i] = { label: term['term'], data: term['count'], color: window.graph_colors[i] };
          });
          var remain = data.slice(window.graph_colors.length,data.length)
          var r = 0
          for (var x in remain) { r += remain[x].data; }
          data = data.slice(0,window.graph_colors.length)
          data.push({ label: "The Rest", data: r, color: '#AAA' })
          pieChart(data,'#piechart')

          break;

        case 'score':
          if (resultjson.hits.count == resultjson.hits.total) {
            var basedon = "<strong>all "
              + resultjson.hits.count + "</strong>"
          } else {
            var basedon = 'the <strong>' +
              resultjson.hits.count + ' most recent</strong>';
          }
          var title = '<h2>Quick analysis of ' +
            '<strong>' + analyze_field + '</strong> field(s) ' +
            '<button class="btn tiny btn-info" ' +
            'style="display: inline-block" id="back_to_logs">back to logs' +
            '</button>' +
            '</h2>' +
            'This analysis is based on ' + basedon +
            ' events for your query in your selected timeframe.<br><br>';
          $('#logs').html(
            title+CreateTableView(analysisTable(resultjson),'logs analysis'));
          sbctl('hide',false)
          graphLoading();
          window.hashjson.graphmode = 'count'
          getGraph(window.interval);
          break;
        case 'trend':
          var basedon = "<strong>" + resultjson.hits.count + "</strong>";
          var title = '<h2>Trend analysis of <strong>' +
            analyze_field + '</strong> field ' +
            '<button class="btn tiny btn-info" ' +
            'style="display: inline-block" id="back_to_logs">back to logs' +
            '</button>' +
            '</h2>' +
            'These trends are based on ' + basedon + ' events from beginning' +
            ' and end of the selected timeframe for your query.<br><br>';
          $('#logs').html(
            title+CreateTableView(analysisTable(resultjson),'logs analysis'));
          sbctl('hide',false)
          graphLoading();
          window.hashjson.graphmode = 'count'
          getGraph(window.interval);
          break;
        case 'mean':
          var title = '<h2>Statistical analysis of <strong>' +
            analyze_field + '</strong> field ' +
            '<button class="btn tiny btn-info" ' +
            'style="display: inline-block" id="back_to_logs">back to logs' +
            '</button>' +
            '</h2>' +
            'Simple computations of a numeric field across your timeframe. ' +
            'The graph above <strong>shows the mean value</strong> ' +
            'of the <strong>'+field+
            '</strong> field over your selected time frame' +
            '<br><br>';
          var tbl = Array(), i = 0, metric;

          resultjson.facets.stats._type = undefined;
          try{
            delete resultjson.facets.stats._type
          }catch(e){}

          for (var obj in resultjson.facets.stats) {
            var metric = Array();
            metric['Statistic'] = obj.charAt(0).toUpperCase() + obj.slice(1);
            metric['Value'] = resultjson.facets.stats[obj];
            tbl[i] = metric;
            i++;
          }
          $('#logs').html(title+CreateTableView(tbl,'logs'));
          sbctl('hide',false)
          graphLoading();
          window.hashjson.graphmode = 'mean'
          getGraph(window.interval);
          break;
        }
      }
    }
  });
}

function graphLoading() {
  $('#graph').html(
    '<center><br><p><img src=' +
    'images/barload.gif></center>');
}

function analysisTable(resultjson) {
  var i = 0;
  var tblArray = new Array();
  for (var obj in resultjson.hits.hits) {
    metric = {},
    object = resultjson.hits.hits[obj];
    metric['Rank'] = i+1;
    var idv = object.id.split('||');
    var fields = window.hashjson.analyze_field.split(',,');
    for (var count = 0; count < fields.length; count++) {
      metric[fields[count]]=xmlEnt(idv[count]);
    }
    var analyze_field = fields.join(' ')
    metric['Count'] = object.count;
    metric['Percent'] =  Math.round(
      metric['Count'] / resultjson.hits.count * 10000
      ) / 100 + '%';
    if (window.hashjson.mode == 'trend') {
      if (object.trend > 0) {
        metric['Trend'] = '<span class=positive>+' +
          object.trend + '</span>';
      } else {
        metric['Trend'] = '<span class=negative>' +
          object.trend + '</span>';
      }
    }
    metric['Action'] =  "<span class='raw'>" + xmlEnt(object.id) + "</span>"+
      "<i data-mode='' data-field='" + analyze_field + "' "+
        "class='msearch icon-search icon-large jlink'></i> " +
      "<i data-mode='analysis' data-field='"+analyze_field+"' "+
        "class='msearch icon-cog icon-large jlink'></i>";

    tblArray[i] = metric;
    i++;
  }
  return tblArray;
}

function termsTable(resultjson) {
  var i = 0;
  var tblArray = new Array();
  for (var obj in resultjson.facets.terms.terms) {
    var object = resultjson.facets.terms.terms[obj];
    var metric = {};
    var color = i < window.graph_colors.length ? 
      " <i class=icon-sign-blank style='color:"+window.graph_colors[i]+"'><i>" : '';

    metric['Rank'] = (i + 1) + color;
    var termv = object.term.split('||');
    var fields = window.hashjson.analyze_field.split(',,');
    for (var count = 0; count < fields.length; count++) {
      // TODO: This is so wrong, really shouldn't be matching a string here
      if (typeof termv[count] === 'undefined' || termv[count] == "null" ) {
        var value = ''
      } else {
        var value = xmlEnt(termv[count])
      }
      metric[fields[count]] = value;
    }
    var analyze_field = fields.join(' ')
    metric['Count'] = addCommas(object.count);
    metric['Percent'] =  Math.round(
      object.count / resultjson.hits.total * 10000
      ) / 100 + '%';
    metric['Action'] =  "<span class='raw'>" + xmlEnt(object.term) + "</span>"+
      "<i data-mode='' data-field='" + analyze_field + "' "+
        "class='msearch icon-search icon-large jlink'></i> " +
      "<i data-mode='analysis' data-field='"+analyze_field+"' "+
        "class='msearch icon-cog icon-large jlink'></i>";

    tblArray[i] = metric;
    i++;
  }
  return tblArray;
}

function setMeta(hits, mode) {
  if ( hits == 'loading' ) {
    $('#meta').html('<img src=images/ajax-loader.gif>');
  } else {
    $('#meta').html(
      addCommas(hits) + " <span class=small>hits</span></td></tr>");
  }
}

function sidebar_field_string(field, mode) {
  var icon = mode == 'selected' ? 'minus' : 'plus';
  return '<li data-field="'+field+'">'+
    '<i class="small icon-'+icon+' jlink mfield" data-field="'+field+'"></i> '+
    '<a style="display:inline-block" class="' + mode +
    ' popup-marker jlink field" rel="popover" data-field="'+field+'">'+ field +
    "<i class='field icon-caret-right'></i></a></li>";
}

function popover_setup() {

  popover_visible = false;
  popover_clickedaway = false;

  $(document).click(function(e) {
    if(popover_visible & popover_clickedaway & !$(e.target).is("a.micro"))
    {
      $('.popover').remove()
      popover_visible = popover_clickedaway = false
    } else {
      popover_clickedaway = true
    }
  });
}

function enable_popovers() {
  $('.popup-marker').popover({
    html: true,
    trigger: 'manual',
    title: function() {
      var field = $(this).attr('data-field');
      var objids = get_objids_with_field(window.resultjson,field);
      var buttons = "<span class='raw'>" + field + "</span>" +
        "<i class='jlink icon-search msearch' data-action='' "+
        "data-field='_exists_'></i> " +
        "<i class='jlink icon-ban-circle msearch' data-action='' "+
        "data-field='_missing_'></i> ";
      var str = buttons + " " + field +
        " <small><span class='small event_count'>"+
        "(<a class='jlink highlight_events' data-field='"+field+"'" +
          " data-mode='field' data-objid='"+objids+"'>" +
          objids.length+" events</a> on this page)</span></small>";  
      return str;
    },
    content: function() {
      var related_limit = 10;
      var field = $(this).attr('data-field');
      var objids = get_objids_with_field(window.resultjson,field);
      var counts = get_related_fields(window.resultjson,field);
      var str = ''

      if(counts.length > 0) {
        str = '<span class=related><small><strong>Related fields:</strong><br> '
        var i = 0
        $.each(counts, function(index,value) {
          var display = i < related_limit ? 'inline-block' : 'none';
          str += "<span style='display:"+display+"'><a data-field='" + value[0] + "' "+
                  "class='jlink micro mfield'>" + value[0] +
                  "</a> (" + to_percent(value[1],objids.length) + "), </span>";
          i++;
        });
        str += (i > related_limit) ? ' <a class="jlink micro more">' +
          (i - related_limit) + ' more</a>' : '';
        str += "</small></span>";
      }

      str =  microAnalysisTable(window.resultjson,field,5) + 
        "<span id=micrograph></span>"+
        str +
        "<div class='btn-group'>" +
          "<button class='btn btn-small analyze_btn' rel='score' " +
          "data-field="+field+"><i class='icon-list-ol'></i> Score</button>" +
          "<button class='btn btn-small analyze_btn' rel='trend' " +
          "data-field="+field+"><i class='icon-tasks'></i> Trend</button>" +
          "<button class='btn btn-small analyze_btn' rel='terms' " +
          "data-field="+field+"><i class='icon-th-list'></i> Terms</button>" +
          "<button class='btn btn-small analyze_btn' rel='mean' " +
          "data-field="+field+"><i class='icon-bar-chart'></i> Stats</button>" +
        "</div>";
      return str;
    },
  }).click(function(e) {
    if(popover_visible) {
      $('.popover').remove();
    }
    $(this).popover('show');
    var data = top_field_values(window.resultjson,$(this).attr('data-field'),5)
    var i = 0, chart = [];
    for (var point in data) {
      chart.push([[data[point][1],0]])
      i = i + 1;
    }
    tiny_bar(chart,'#micrograph')
    popover_clickedaway = false
    popover_visible = true
    e.preventDefault()

  });
}

function microAnalysisTable (json,field,count) {
  var counts = top_field_values(json,field,count)
  var table = []
  var colors = window.graph_colors;
  var i = 0;
  $.each(counts, function(index,value){
    var show_val = value[0] == '' ? '<i>blank</i>' : xmlEnt(value[0]);
    var objids = get_objids_with_field_value(window.resultjson,field,value[0])
    var field_val = "<i class=icon-sign-blank style='color:"+colors[i]+"'></i> "+
    "<a class='jlink highlight_events' data-mode='value'"+
    " data-field='"+field+"' data-objid='"+objids+"'>"+show_val+"</a>";
    var buttons = "<span class='raw'>" + xmlEnt(value[0]) + "</span>" +
              "<i class='jlink icon-large icon-search msearch'"+
              " data-action='' data-field='"+field+"'></i> " +
              "<i class='jlink icon-large icon-ban-circle msearch'"+
              " data-action='NOT ' data-field='"+field+"'></i> ";
    var percent = "<strong>" +
      to_percent(value[1],window.resultjson.kibana.per_page) +"</strong>";
    table.push([field_val,percent,buttons]);
    i = i+1;
  });
  return CreateTableView(table,
    'table table-condensed table-bordered micro',false,['99%','30px','30px'])
}

function pageLinks() {
  // Pagination
  var perpage = window.resultjson.kibana.per_page
  var str = "<table class='pagelinks'><tr>";
  var end = window.hashjson.offset + window.resultjson.hits.hits.length;
  if (end < resultjson.hits.total)
  {
    //str += "<i data-action='nextpage' class='page icon-arrow-right jlink'></i> "
    str += "<td width='1%'><a data-action='nextpage' class='page jlink'>Older</a></td>"
  }
  str += "<td width='99%'><strong>" + window.hashjson.offset + " TO " + end + "</strong></td>";
  if (window.hashjson.offset - perpage >= 0) {
    //str += "<i data-action='firstpage' " +
    //  "class='page jlink icon-circle-arrow-left'></i> " +
    //  "<i data-action='prevpage' class='page icon-arrow-left jlink'></i> ";
    str += "<td width='1%'><a data-action='prevpage' class='page jlink'>Newer</a></td> " +
    "<td width='1%'> <a data-action='firstpage' class='page jlink'>Newest</a></td>";
  }
  str += "</tr></table>";

  $('.pagelinks').html(str);
}

// This is very ugly
function blank_page() {
  var selectors = ['#graph','#graphheader','#feedlinks','#logs','.pagelinks',
    '#fields','#analyze']

  for (var selector in selectors) {
    $(selectors[selector]).text("");
  }
}

// This function creates a standard table with column/rows
// objArray = Anytype of object array, like JSON results
// theme (optional) = A css class to add to the table
// enableHeader (optional) = Controls if you want to hide/show, default is show
// widths = control the widths of columns (optional)
function CreateTableView(objArray, theme, enableHeader, widths) {

  if (theme === undefined) theme = 'mediumTable'; //default theme
  if (enableHeader === undefined) enableHeader = true; //default enable header

  // If the returned data is an object do nothing, else try to parse
  var array = typeof objArray != 'object' ? JSON.parse(objArray) : objArray;

  var str = '<table class="' + theme + '">';

    // table head
  if (enableHeader) {
    str += '<thead><tr>';
    for (var index in array[0]) {
      str += '<th scope="col">' + field_slim(index) + '</th>';
    }
    str += '</tr></thead>';
  }

    // table body
  str += '<tbody>';
  for (var i = 0; i < array.length; i++) {
    str += (i % 2 == 0) ? '<tr class="alt">' : '<tr>';
    for (var index in array[i]) {
      var width = (!(widths === undefined)) ? 'width="'+widths[index]+'"' : '';
      str += '<td '+width+'>' + array[i][index] + '</td>';
    }
    str += '</tr>';
  }
  str += '</tbody></table>';
  return str;
}


// This function creates a table of LOGS with column/rows
// objArray = Anytype of object array, like JSON results
// fields = Of the fields returned, only display these
// theme (optional) = A css class to add to the table
// enableHeader (optional) = Controls if you want to hide/show, default is show

function CreateLogTable(objArray, fields, theme, enableHeader) {
  // set optional theme parameter
  theme = theme === undefined ? 'mediumTable' : theme;
  enableHeader = enableHeader === undefined ? true : false;

  if (objArray === undefined) {
    return "<center>" +
      "No results match your query. Please try a different search" +
      "</center><br>";
  }

  if (fields.length == 0)
    fields = window.resultjson.kibana.default_fields;

  // If the returned data is an object do nothing, else try to parse
  var array = typeof objArray != 'object' ? JSON.parse(objArray) : objArray;

  // Remove empty items from fields array
  fields = $.grep(fields, function (n) {
    return (n);
  });

  var str = "<div class='pagelinks'></div>";
  str += '<table class="' + theme + '">';

  // table head
  if (enableHeader) {
    str += '<thead><tr>';
    str += '<th scope="col" class=firsttd>Time</th>';
    for (var index in fields) {
      var field = fields[index];
      str += '<th scope="col" class="column" data-field="'+field+'">' +
      '<i data-mode="left" class="shift_column jlink icon-caret-left"></i>' +
      '<div style="display:inline-block;text-align:center">'+field_slim(field) + '</div>'+
      ' <i data-mode="right" class="shift_column jlink icon-caret-right"></i></th>';
    }
    str += '</tr></thead>';
  }

  // table body
  str += '<tbody>';
  var i = 1;
  for (var objid in array) {
    var object = array[objid];
    for(var key in object.highlight) {
      var hlfield = key;
      var hlvalue = object.highlight[hlfield];
    }

    var id = object._id;
    var alt = i % 2 == 0 ? '' : 'alt'
    var time = prettyDateString(
      Date.parse(get_field_value(object,window.timestamp)) + tOffset);
    str += '<tr data-object="' + objid + '" id="logrow_' + objid + '" '+
      'class="' + alt + ' logrow">';

    str += '<td class=firsttd>' + time + '</td>';
    for (var index in fields) {
      var field = fields[index];
      if (typeof hlfield === "undefined") 
        var value = get_field_value(object,field);
      else
      {
        if ( field.toString() == hlfield.toString() ) 
          var value = hlvalue;
        else
          var value = get_field_value(object,field);
      }

      var value = value === undefined ? "-" : value.toString();
      var value = xmlEnt(wbr(value),10);
      var value = value.replace(RegExp("@KIBANA_HIGHLIGHT_START@(.*?)@KIBANA_HIGHLIGHT_END@", "g"),
	    function (all, text, char) {
	      return "<span class='highlightedtext'>" + text + "</span>";
	    }
	);
      str += '<td class="column" data-field="'+field+'">' +
        value + '</td>';
    }
    str += '</tr><tr class="hidedetails"></tr>';
    hlfield = undefined;
    i++;
  }

  str += '</tbody></table>';
  str += "<div class='pagelinks'></div>";
  return str;
}

// Create a table with details about an object
function details_table(objid,theme) {
  if (theme === undefined) theme = 'logdetails table table-bordered';

  var obj = window.resultjson.hits.hits[objid];

  //obj_fields = get_object_fields(obj);
  var obj_fields = flatten_json(obj['_source'])
  var str = "<table class='"+theme+"'>" +
    "<tr><th>Field</th><th>Action</th><th>Value</th></tr>";


  var orig = '';

  var i = 1;
  for (index in obj_fields) {
    var field = index
    var value = obj_fields[index];
    var field_id = field.replace('@', 'ATSYM');
    //var value = get_field_value(obj,field);
    var buttons = "<span class='raw'>" + xmlEnt(value) + "</span>" +
      "<i class='jlink icon-large icon-search msearch' " +
      "data-action='' data-field='"+field+"'></i> " +
      "<i class='jlink icon-large icon-ban-circle msearch' " +
      "data-action='NOT ' data-field='"+field+"'></i> ";

    var trclass = (i % 2 == 0) ?
      'class="alt '+field_id+'_row"' : 'class="'+field_id+'_row"';

    // Are URLs clickable ?
    if (resultjson.kibana.clickable_urls) {
      var value = value === undefined ? "-" : value.toString();
      // Detect URLs and inserts delimiters
      var value = value.replace(RegExp("(https?://([-\\w\\.]+)+(:\\d+)?(/([-\\w/_\\.]*(\\?\\S+)?)?)?)", "g"),
        function (all, text, char) {
          return "@KIBANA_LINK_START@" + text + "@KIBANA_LINK_END@";
        }
      );
      var value = xmlEnt(wbr(value),10);
      // Replace delimiters by HTML code
      var value = value.replace(RegExp("@KIBANA_LINK_START@(.*?)@KIBANA_LINK_END@", "g"), 
        function (all, text) {
          // Clean link
          var stripped = text.replace( new RegExp("<del>&#8203;</del>","g"),"");
          return "<a href='" + stripped + "' target='_blank'>" + text + "</a>";
        }
      );
      str += "<tr " + trclass + ">" +
	"<td class='firsttd " + field_id + "_field'>" + field + "</td>" +
	"<td style='width: 60px'>" + buttons + "</td>" +
	'<td>' + value +
	"</td></tr>";
    } else {
      str += "<tr " + trclass + ">" +
        "<td class='firsttd " + field_id + "_field'>" + field + "</td>" +
        "<td style='width: 60px'>" + buttons + "</td>" +
        '<td>' + xmlEnt(wbr(value, 10)) +
        "</td></tr>";
    }

    i++;

  }
  str += "</table>";
  return str;
}

function mSearch(field, value, mode) {
  window.hashjson.offset = 0;
  if (mode === undefined) mode = '';
  if (mode != 'analysis') {
    window.hashjson.mode = mode;
    window.hashjson.analyze_field = '';
  }
  var pattern=/^(.*)\|([^"']*)$/;
  var queryinput=$('#queryinput').val();
  if (pattern.test(queryinput) == true) {
    var results = queryinput.match(pattern);
    var queryinput = $.trim(results[1]);
    var fields = $.trim(results[2]).split(/\s+/).slice(1);
    var values = value.toString().split('||');
    var query = '';
    var glue = ''
    // TODO: This only works if a query already exists I think?
    for (var count = 0;count < fields.length;count++) {
      value = values[count];
      if (value == "null" || typeof value === "undefined") {
        value = fields[count];
        field = '_missing_'
      } else {
        field = fields[count];
      }
      query = query + glue + field + ":" + "\"" + addslashes(value.toString()) + "\"";
      glue = " AND ";
    }
  } else {
    var query = field + ":" + "\"" + addslashes(value.toString()) + "\"";
  }
  var glue = queryinput != "" ? " AND " : " ";
  window.hashjson.search = queryinput + glue + query;
  setHash(window.hashjson);
  scroll(0, 0);
}

function field_alias(field) {
  return field.replace('@', 'ATSYM');
}

function field_slim(field) {
  return field.replace(/(.*)\.(.*)/,"<span class=small>$1.</span><br>$2");
}

function mFields(field) {
  // If the field is not in the hashjson, add it
  if ($.inArray(field, window.hashjson.fields) < 0) {

    // We're adding a field, but there's nothing in the hashjson,
    // remove default fields
    if (window.hashjson.fields.length == 0) {
      $('#logs').find('tr.logrow').each(function(){
        $('.column').remove();
      });
    }

    // Add field to hashjson
    window.hashjson.fields.push(field);
    $('#fields ul.unselected a[data-field="' + field + '"]').addClass('selected');
    $('#fields ul.unselected i[data-field="' + field + '"]').removeClass('icon-plus');
    $('#fields ul.unselected i[data-field="' + field + '"]').addClass('icon-minus');

    // Add column
    $('#logs').find('tr.logrow').each(function(){
        var obj = window.resultjson.hits.hits[$(this).attr('data-object')];
        var value = get_field_value(obj,field)
        $(this).find('td').last().after(
          '<td class="column" data-field="'+field+'">' + xmlEnt(wbr(value, 10)) + '</td>');
    });
    $('#logs thead tr').find('th').last().after(
      '<th scope="col" class="column" data-field="'+field+'">' +
      '<i data-mode="left" class="shift_column jlink icon-caret-left"></i>' +
      '<div style="display:inline-block;text-align:center">'+field_slim(field) + '</div>'+
      ' <i data-mode="right" class="shift_column jlink icon-caret-right"></i></th>'
      );

  } else {
    $('#logs .column[data-field="'+field+'"]').remove();

    // Otherwise, remove it
    window.hashjson.fields = jQuery.grep(
      window.hashjson.fields, function (value) {
        return value != field;
      }
    );

    // Remove from selected, add to unselected
    $('#fields ul.unselected a[data-field="' + field + '"]').removeClass('selected');
    $('#fields ul.unselected i[data-field="' + field + '"]').removeClass('icon-minus');
    $('#fields ul.unselected i[data-field="' + field + '"]').addClass('icon-plus');

    if (window.hashjson.fields.length == 0) {
      $.each(window.resultjson.kibana.default_fields, function(index,field){
        $('#logs').find('tr.logrow').each(function(){
          var obj = window.resultjson.hits.hits[$(this).attr('data-object')];
          var value = get_field_value(obj,field)
          $(this).find('td').last().after(
            '<td class="column" data-field="'+field+'">' +
            xmlEnt(wbr(value, 10)) + '</td>');
        });
        $('#logs thead tr').find('th').last().after(
          '<th scope="col" class="column" data-field="'+field+'">' +
          field_slim(field) + '</th>');
      });
    }

  }

  // Remove empty items if they exist
  window.hashjson.fields = $.grep(window.hashjson.fields,function(n){
    return(n);
  });

  $('#feedlinks').html(feedLinks(window.hashjson));

  enable_popovers();
  pageLinks();
}

function feedLinks(obj) {
  /*
  return "<a href=rss/" + Base64.encode(JSON.stringify(obj, null, '')) +">rss " +
    "<i class='icon-rss'></i></a> "+
    "<a href=export/" + Base64.encode(JSON.stringify(obj, null, '')) + ">export " +
    "<i class='icon-hdd'></i></a> "+
    "<a href=stream#" + Base64.encode(JSON.stringify(obj, null, '')) + ">stream " +
    "<i class='icon-dashboard'></i></a>"
  */
  return "<a href=stream#" + Base64.encode(JSON.stringify(obj, null, '')) + ">stream " +
    "<i class='icon-dashboard'></i></a>"
}

$(function () {
  $('form').submit(function () {

    if (window.hashjson.search != $('#queryinput').val()) {
      window.hashjson.offset = 0;
      window.hashjson.search = $('#queryinput').val();
    }
    window.hashjson.stamp = new Date().getTime();

    if (window.hashjson.timeframe == "custom")
      $('#timechange').click();
    else
      window.hashjson.timeframe = $('#timeinput').val();

   var pattern=/^(.*)\|([^"']*)$/;
   if (pattern.test(window.hashjson.search) == true) {
      var results = window.hashjson.search.match(pattern);
      var search  = $.trim(results[1]);
      var fields  = $.trim(results[2]).split(/\s+/);
      var field   = fields.slice(1).join(',,');
      var mode    = fields[0];

      window.hashjson.mode = mode;
      if (mode == 'columns')
        window.hashjson.fields = field.split(',,')
      else
        window.hashjson.analyze_field = field;
    }

    if (window.location.hash == "#" + JSON.stringify(window.hashjson, null, ''))
      pageload(window.location.hash);
    else
      setHash(window.hashjson);

    return false;
  });
});

function datepickers(from,to) {

  var graph_interval = window.hashjson.time.user_interval;

  var interval_opts = {
    auto:0,
    second:1000,
    minute:60*1000,
    hour:60*60*1000,
    day:60*60*24*1000
  };
  var options = ''
  $.each(interval_opts,function(i,interval) {
    options += '<option value='+interval+(interval == graph_interval ? ' selected' : '') +
      '>'+i+'</option>';
  });

  $('#graphheader').html("<div class='form-inline'>"+
    "<input size=19 id=timefrom class='datetimeRange'" +
    " type=text name=timefrom> to " +
    "<input size=19 id=timeto class='datetimeRange'" +
    " type=text name=timeto> grouped by " +
    "<select id=user_interval name=user_interval> " + options + "</select>" +
    "<button id='timechange' class='btn btn-small jlink' " + 
    "style='visibility: hidden'> filter</button></div>");

  var from_date = utc_date_obj(new Date(from - tOffset))
  var to_date   = utc_date_obj(new Date(to - tOffset));
  $('#timefrom').datetimeEntry({
    maxDatetime : to_date,
    datetimeFormat: 'Y-O-D H:M:S',
    spinnerImage: ''
  });
  $('#timefrom').datetimeEntry('setDatetime',from_date)

  $('#timeto').datetimeEntry({
    minDatetime: $('#timefrom').datetimeEntry('getDatetime'),
    maxDatetime: utc_date_obj(new Date()),
    datetimeFormat: 'Y-O-D H:M:S',
    spinnerImage: ''
  },to);
  $('#timeto').datetimeEntry('setDatetime',to_date)


  // LOL Wat? o_from and o_to are globals?! I should be beaten with a hose for 
  // the horrid way time is handled in this application. Stupid FLOT.
  $('#timefrom,#timeto').datepicker({
    format: 'yyyy-mm-dd'
  }).on('show', function(ev) {
    o_from = local_date_obj(
      new Date($('#timefrom').datetimeEntry('getDatetime').getTime() - tOffset));
    o_to = local_date_obj(
      new Date($('#timeto').datetimeEntry('getDatetime').getTime() - tOffset));
  });
}

// Render the date/time picker
// Must make this pretty
function renderDateTimePicker(from, to, force) {
  $('.datepicker').remove()

  from = from + tOffset
  to = to + tOffset

  if (!$('#timechange').length || force == true) {

    datepickers(from,to)

    $('#timefrom').datepicker().on('changeDate', function(ev) {
      o_from.setUTCFullYear(ev.date.getFullYear())
      o_from.setUTCMonth(ev.date.getMonth())
      o_from.setUTCDate(ev.date.getDate())
      $('.datepicker').remove()
      renderDateTimePicker(
        o_from.getTime() + tOffset,
        o_to.getTime() + tOffset,
        true
      );
      window.hashjson.timeframe = 'custom'
      $('#timeinput').val('custom');
    })

    $('#timeto').datepicker().on('changeDate', function(ev) {
      o_to.setUTCFullYear(ev.date.getFullYear())
      o_to.setUTCMonth(ev.date.getMonth())
      o_to.setUTCDate(ev.date.getDate())
      $('.datepicker').remove()
      renderDateTimePicker(
        o_from.getTime() + tOffset,
        o_to.getTime() + tOffset,
        true
      );
      window.hashjson.timeframe = 'custom'
      $('#timeinput').val('custom');
    })

    $('input.datetimeRange').datetimeEntry({datetimeFormat: 'Y-O-D H:M:S'}).
    change(function() {
      $('#' + (this.id == 'timefrom' ? 'timeto' : 'timefrom')).datetimeEntry(
        'change', (this.id == 'timefrom' ? 'minDatetime' : 'maxDatetime'),
        $(this).datetimeEntry('getDatetime'));
    });

    $('#user_interval').change(function () {
      var interval = $('#user_interval').val()
      if(typeof window.hashjson.time == 'undefined')
        window.hashjson.time = {user_interval:interval};
      else
        window.hashjson.time.user_interval = interval;
    });

    $('#timefrom,#timeto').change(function () {
      var time = {
        "from": field_time('#timefrom'),
        "to": field_time('#timeto'),
        "user_interval": $('#user_interval').val()
      };
      window.hashjson.offset = 0;
      window.hashjson.time = time;
      $('#timeinput').val('custom');
      $('#timeinput').change();
    });

    // Give user a nice interface for selecting time ranges
    $("#timechange").click(function () {
      var time = {
        "from": field_time('#timefrom'),
        "to": field_time('#timeto'),
        "user_interval": $('#user_interval').val()
      };
      window.hashjson.offset = 0;
      window.hashjson.time = time;
      window.hashjson.timeframe = "custom";
      setHash(window.hashjson);
    });
  }
}

function field_time(selector) {
  var tz_offset = int_to_tz(window.tOffset);
  var str = ISODateString(
    new Date($(selector).datetimeEntry('getDatetime').getTime())
    ) + tz_offset;
  return str;
}


function tiny_bar(data,selector) {
  $.plot( $(selector),data, {
    series: {
      stack: true,
      lines: { show:false },
      bars: { show: true, horizontal:true, fill: 1 }
    },
    xaxis: {show:false, max: window.resultjson.kibana.per_page},
    yaxis: {show:false},
    grid: {show:false},  
  });
}


// Big horrible function for creating graphs
function logGraph(data, interval, metric) {

  // If mode is graph, graph count, otherwise remove word 'graph' and chart
  // whatever is left. ie meangraph -> mean
  if (typeof metric === 'undefined')
    metric = 'count';

  metric = metric.replace('graph','');

  if (metric === '')
    metric = 'count';

  var array = new Array();
  var from, to;
  if(typeof window.resultjson.kibana.time !== 'undefined') {
    // add null value at time from.
    if(window.hashjson.timeframe != 'all') {
      from = Date.parse(window.resultjson.kibana.time.from) + tOffset
      array.push(
        Array(from, null));
    }
  }

  for (var index in data) {
    var value = data[index][metric];
    array.push(Array(data[index].time + tOffset, value));
  }

  if(typeof window.resultjson.kibana.time !== 'undefined') {
    // add null value at time to.
    to = Date.parse(window.resultjson.kibana.time.to) + tOffset
    array.push(
      Array(to, null));
  }

  from = array[0][0];
  to = array[array.length -1][0]

  renderDateTimePicker(from,to,true);

  // Make sure we get results before calculating graph stuff
  if (!jQuery.isEmptyObject(data)) {

    // Allow user to select ranges on graph.
    // Its this OR click, not both it seems.
    var intset = false;
    $('#graph').bind("plotselected", function (event, ranges) {
      if (!intset) {
        intset = true;
        var from = utc_date_obj(new Date(parseInt(ranges.xaxis.from.toFixed(0))))
        var to = utc_date_obj(new Date(parseInt(ranges.xaxis.to.toFixed(0))))
        var time = {
          "from": ISODateString(from)+int_to_tz(window.tOffset),
          "to": ISODateString(to)+int_to_tz(window.tOffset)
        };
        window.hashjson.offset = 0;
        window.hashjson.time = time;
        window.hashjson.timeframe = "custom";
        setHash(window.hashjson);

      }
    });

    // Allow user to hover over a bar and get details
    var previousPoint = null;
    $("#graph").bind("plothover", function (event, pos, item) {
      $("#x").text(pos.x.toFixed(2));
      $("#y").text(pos.y.toFixed(2));

      if (item) {
        if (previousPoint != item.dataIndex) {
          previousPoint = item.dataIndex;

          $("#tooltip").remove();
          var x = item.datapoint[0].toFixed(0),
            y = Math.round(item.datapoint[1]*100)/100;

          showTooltip(
            item.pageX-120, item.pageY-20, y + " at " + prettyDateString(x)
          );
        }
      } else {
        $("#tooltip").remove();
        previousPoint = null;
      }
    });

    var label = 'Logs';
    if (metric !== '')
      label = metric;

    var color = getGraphColor(metric);
    $.plot(
    $("#graph"), [
    {
      data: array,
      label: label + " per " + secondsToHms(parseInt(interval) / 1000)
    }
    ], {
      legend: { position: "nw" },
      series: {
        lines:  { show: false, fill: true },
        bars:   { show: true,  fill: 1, barWidth: interval / 1.7 },
        points: { show: false },
        color: color,
        shadowSize: 1
      },
      xaxis: {
        mode: "time",
        timeformat: "%H:%M:%S<br>%m-%d",
        label: "Datetime",
        color: "#000",
      },
      yaxis: {
        min: 0,
        color: "#000"
      },
      selection: {
        mode: "x",
        color: '#000'
      },
      grid: {
        backgroundColor: '#fff',
        borderWidth: 0,
        borderColor: '#000',
        color: "#ddd",
        hoverable: true,
        clickable: true
      }
    });
  }
  $("#graph_container").resizable({
    minHeight: 100,
    handles: 's',
    stop: function(event, ui) {
      $("#graph_container").css('width', '');
    }
  });
}

function showTooltip(x, y, contents) {
  $('<div id="tooltip">' + contents + '</div>').css({
    position: 'absolute',
    display: 'none',
    top: y,
    left: x,
    color: '#eee',
    border: '1px solid #fff',
    padding: '3px',
    'font-size': '8pt',
    'background-color': '#000',
    border: '1px solid #000',
    'font-family': '"Verdana", Geneva, sans-serif'
  }).appendTo("body").fadeIn(200);
}

function sbctl(mode,user_selected) {
  var sb = $('#sidebar'),
    main = $('#main'),
    lnk = $('#sbctl'),
    win = $(window);
  if(user_selected) {
    window.sb = mode;
  }
  if (mode == 'hide') {
    // collapse
    sb.hide();
    main.removeClass('span10');
    main.addClass('span12');
    lnk.removeClass('ui-icon-triangle-1-w');
    lnk.addClass('ui-icon-triangle-1-e');
    main.addClass('sidebar-collapsed');
    win.smartresize();
  }
  if (mode == 'show') {
    sb.show();
    main.removeClass('span12');
    main.addClass('span10');
    lnk.removeClass('ui-icon-triangle-1-e');
    lnk.addClass('ui-icon-triangle-1-w');
    main.removeClass('sidebar-collapsed');
    win.smartresize();
  }
}

function move_column(field,dir) {
  var x = dir == 'right' ? 1 : -2
  var len = $('#logs thead th').length
  if (len == 2)
    return
  var thi = $('#logs th.column[data-field="'+field+'"]').index()

  dest = thi+x
  if (x == -2 && thi == 1)
    dest = len-1
  if (x == 1 && thi == (len-1))
    dest = 0


  var th1 = $('#logs thead th:eq('+thi+')')
  var th2 = $('#logs thead th:eq('+dest+')')
  th1.detach().insertAfter(th2)

  $('#logs tr.logrow').each(function() {
    var tr = $(this);
    var td1 = tr.find('td:eq('+thi+')');
    var td2 = tr.find('td:eq('+dest+')');
    td1.detach().insertAfter(td2);
  });


  window.hashjson.fields = $('#logs th.column').map(
    function(){return $(this).attr("data-field");}
    ).get()
}

function showError(title,text) {
  blank_page();
  $('#logs').html("<h2>"+title+"</h2>"+text);

  // We have to use hashjson's time here since we won't
  // get a resultjson on error, usually
  if(
    typeof window.hashjson.time.from !== 'undefined' &&
    typeof window.hashjson.time.to !== 'undefined'
    ) {
    renderDateTimePicker(
      Date.parse(window.hashjson.time.from)+tOffset,
      Date.parse(window.hashjson.time.to)+tOffset
    );
  }
  sbctl('hide')
}

function getGraphColor(mode) {
  switch(mode)
  {
  case "mean":
    var color = '#ef9a23';
  break;
  default:
    var color = '#5aba65';
  }
  return color;
}

function resetAll() {
  window.hashjson = JSON.parse(
    '{'+
      '"search":"",'+
      '"fields":[],'+
      '"offset":0,'+
      '"timeframe":900,'+
      '"graphmode":"count"'+
    '}'
  );

  setHash(window.hashjson);
}

function highlight_events(objids) {
  for (objid in objids) {
    $('#logs tr#logrow_'+objids[objid]).addClass('highlight')
  }
}

function unhighlight_events(objids) {
  for (objid in objids) {
    $('#logs tr#logrow_'+objids[objid]).removeClass('highlight')
  }
}

function unhighlight_all_events() {
  $('#logs .highlight').removeClass('highlight');
}

function bind_clicks() {

  $('body').delegate("i.shift_column", "click",
    function () {
      move_column($(this).parent().attr('data-field'),$(this).attr('data-mode'))
    });

  // Side bar expand/collapse
  $('#sbctl').click(function () {
    var sb = $('#sidebar');
    if (sb.is(':visible')) {
      sbctl('hide',true);
    } else {
      sbctl('show',true);
    }
  });

  // Reset button
  $("#resetall").click(function () {
    window.location.hash = '#';
  });


  // Time changes
  $('#timeinput').change(function () {
    window.hashjson.timeframe = $(this).val();
    if (window.hashjson.timeframe == "custom") {
      //Initialize the date picker with a 15 minute window into the past
      var d = new Date()
      var startDate = new Date(d - (15 * 60 * 1000));
      renderDateTimePicker(
        startDate, d);
    }
  });

  // Go back to the logs
  $("#logs").delegate("button#back_to_logs", "click",
    function () {
      var pattern=/^(.*)\|([^"']*)$/;
      if (pattern.test(window.hashjson.search) == true) {
         var results = window.hashjson.search.match(pattern);
         window.hashjson.search = $.trim(results[1]);
      }
      window.hashjson.mode = '';
      window.hashjson.graphmode = 'count';
      window.hashjson.analyze_field = '';
      setHash(window.hashjson);
    }
  );

   $("#logs").delegate("tr.logrow", "click",
    function () {
      $(this).next().html(
        '<td colspan=100>' + details_table($(this).attr('data-object')) +'</td>'
      );
      $($(this).next()).toggleClass('hidedetails');
    }
  );


  $("#logs").delegate("a.page", "click",
    function () {
      var per_page = window.resultjson.kibana.per_page;
      var action = $(this).attr('data-action')
      switch (action) {
      case 'nextpage':
        window.hashjson.offset += per_page;
      break;
      case 'prevpage':
        window.hashjson.offset = window.hashjson.offset - per_page;
      break;
      case 'firstpage':
        window.hashjson.offset = 0;
      break;
      }
      setHash(window.hashjson);
  });

  // Sidebar analysis stuff
  $(document).delegate(".popover .analyze_btn", "click", function () {
    window.hashjson.offset = 0;
    var mode  = $(this).attr('rel');
    var field = $(this).attr('data-field');
    analyzeField(field, mode)
  });

  // Analysis table rescore
  $("body").delegate("i.msearch", "click", function () {
    var action = typeof $(this).attr('data-action') === 'undefined' ?
      '' : $(this).attr('data-action');
    var mode = $(this).attr('data-mode');
    var field = $(this).attr("data-field");
    var value = $(this).parent().children('span.raw').text();
    if (value == '') {
      value = field;
      field = '_missing_';
    }
    mSearch(action + field, value, mode);
  });

  // Column selection
  $("body").delegate(".mfield", "click", function () {
    mFields($(this).attr('data-field'));
  });

  // Column filter
  $("body").delegate('#field_filter','keyup',function(e){
      var search = $(this).val().toLowerCase();
      if (!search || e.keyCode == 27) { //esc key
          $(this).val('');
          window.field_list.show();
          return;        
      }
      
      window.field_list.hide();
      var shown = window.field_list.filter(function(index) {
          return ($(this).attr('data-field').toLowerCase().indexOf(search) !== -1);
      }).show();
      
      if (shown.length == 1 && e.keyCode == 13) { // enter key
          shown.children('i').click(); //toggle column
          $(this).val('');
          window.field_list.show();       
      }
  });

  $("body").delegate("span.related a.more", "click", function () {
    $('span.related span').show();
    $('span.related a.more').remove();
    $('div.popover div.arrow').remove();
  });

  $("body").delegate("a.highlight_events", "click", function () {
    unhighlight_all_events();
    $('.alert-highlight').remove();
    var objids = $(this).attr('data-objid').split(',');
    var field  = $(this).attr('data-field');
    if ($(this).attr('data-mode') == 'field')
      var notice = 'Highlighting <strong>' + objids.length + ' events</strong>'+
        ' containing the <strong>' + field + '</strong>' +
        ' field. Dismiss this notice to clear highlights.';
    if ($(this).attr('data-mode') == 'value')
      var notice = 'Highlighting <strong>'+objids.length+' events</strong>' +
        ' where <strong>' + field + '</strong> is <strong>' + xmlEnt($(this).text()) +
        '</strong>. Dismiss this notice to clear highlights.';

    $('#logs').prepend(
      '<div class="alert alert-info alert-highlight">' +
        '<button type="button" class="unhighlight close" data-field="' + field +
         '" data-dismiss="alert">' + '×</button>' + notice + '</div>');
    highlight_events(objids);
  });

  $("body").delegate("button.unhighlight", "click", function () {
    unhighlight_all_events();
  });

}
