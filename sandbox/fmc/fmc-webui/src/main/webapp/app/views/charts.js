/*

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

*/
define(["frameworks"], function() {
  
  var updateLegendTimeout = null;

  function updateLegend(el) {
    updateLegendTimeout = null;
    $(el).each(function(index) {
      var state = $(this).data('chart_state');
        if( state ) {
        var pos = state.latestPosition ? state.plot.c2p(state.latestPosition) : null;
        var plot = state.plot;
        if( !plot ) {
          return;
        }

        var axes = plot.getAxes();
        var legends = state.placeholder.find(".legendLabel");

        if (pos==null || pos.x < axes.xaxis.min || pos.x > axes.xaxis.max || pos.y < axes.yaxis.min || pos.y > axes.yaxis.max) {
          var i, dataset = plot.getData();
          for (i = 0; i < dataset.length; ++i) {
            var series = dataset[i];
            legends.eq(i).text(series.label);
          }
          if( state.options.legend.hidden ) {
            state.placeholder.find('.legend').each(function () {
              $(this).css('display', 'none');
            });
          }
        } else {
          var i, j, dataset = plot.getData();
          for (i = 0; i < dataset.length; ++i) {
            var series = dataset[i];

            // find the nearest points, x-wise
            for (j = 0; j < series.data.length; ++j)
              if (series.data[j][0] > pos.x)
                break;

            // now interpolate
            var y, p1 = series.data[j - 1], p2 = series.data[j];
            if (p1 == null)
                y = p2[1];
            else if (p2 == null)
                y = p1[1];
            else
                y = p1[1] + (p2[1] - p1[1]) * (pos.x - p1[0]) / (p2[0] - p1[0]);

            legends.eq(i).text(series.olabel + " = "+state.options.yaxis.tickFormatter(y));
          }
          if( state.options.legend.hidden ) {
            state.placeholder.find('.legend').each(function () {
              $(this).css('display', 'block');
            });
          }

        }
      }
    });
  }
  
  var no_formater = function (val, axis) {
    return val;
  };

  function initialize_charts(el, options) {
    $(el).each(function(index) {
      var placeholder = $(this);
      var state = { placeholder:placeholder, options: {}, fetches:[], data:[], plot:0, latestPosition:null, sources:{}, calcs:[] };
      placeholder.data('chart_state', state);
      
      _.each(options.data, function(data) {
        var url = data.ref;
        var fetch = {
          monitored_set:data.set,
          start: 0,
          end: 0,
          step: 0,
          data_sources:[],
          consolidations:[]
        };
        _.each(data.sources, function(source) {
          fetch.data_sources.push(source.id)
          state.sources[source.id] = source
        });
        state.fetches.push({url:url, post:$.toJSON(fetch)});
      });

      _.each(options.calcs, function(calc) {
        state.calcs.push(calc);
        state.sources[calc.id] = calc
      });

      state.options = {
        legend: {
          position: "nw",
          hidden:true,
          show:true,
          backgroundOpacity:0
        },
        series: {
          lines: { show: true },
          shadowSize: 0
        },
        crosshair: { mode: "x" },
        yaxis: { tickFormatter: no_formater},
        xaxis: { mode: "time" },
        lines: { show: true },
        grid: { hoverable: true, autoHighlight: false }
      };

      if( options.max ) {
        state.options.yaxis.max = options.max;
      }
      if( placeholder.attr('min') ) {
        state.options.yaxis.min = placeholder.attr('min');
      }
      if( options.legend =="true" ) {
        state.options.legend.hidden = false;
      }
      if( options.formater ) {
        state.options.yaxis.tickFormatter = options.formater ;
      }

      function trigger_legend_update(event, pos, item) {
        var state = $(this).data('chart_state');
        state.latestPosition = pos ? state.plot.p2c(pos) : null;
        if (!updateLegendTimeout) {
          updateLegendTimeout = setTimeout(function (){updateLegend(el);}, 50);
        }
      }
      placeholder.bind("plothover",  trigger_legend_update);
      placeholder.mouseout(trigger_legend_update);
    });
  }

  function get_line_data(data, name) {
      for( i in data ) {
        if( data[i].id == name )
          return data[i];
      }
      return false;
  }

  function update_charts(el) {
    $(el).each(function(index) {
      var state = $(this).data('chart_state');
      for( i in state.fetches ) {
        $.ajax({
          type: 'POST',
          url: state.fetches[i].url,
          contentType:'application/json',
          data: state.fetches[i].post,
          processData: false,
          context: state,
          dataType: 'text json',
          success: function (data_set) {
            var now = new Date();
            var state = this;

            function add_data(source, source_data) {
              var line = get_line_data(state.data, source.id);
              if( line ) {
                line.data = source_data;
              } else {
                line = {lines:{}, id:source.id, label:source.label, olabel:source.label, description:source.description, data:source_data };
                if( state.sources[source.id].stack ) {
                  line.stack = state.sources[source.id].stack;
                  line.lines.fill =  true;
                }
                line.hidden = state.sources[source.id].hidden
                state.data.push(line)
              }
            }

            // add the data points from the ajax data...
            for (i in data_set.data_sources) {
              var source = data_set.data_sources[i]
              var source_data = []
              for (j in source.data) {
                var t = data_set.start;
                t -= now.getTimezoneOffset()*60;
                t += (data_set.step*j);
                t *= 1000;
                source_data.push([t, source.data[j]]);
              }
              add_data(source, source_data)
            }

            // add any calculated data points..
            if( state.calcs.length > 0 ) {
              var map = {}
              for( i in state.data) {
                var series = state.data[i];
                if( series.id ) {
                  map[series.id] = series
                }
              }
              for (i in state.calcs) {
                var source = state.calcs[i];
                var source_data = source.apply(map, state);
                if( source_data ) {
                  add_data(source, source_data);
                }
              }
            }

            // To support hiding lines..
            var data = []
            for( i in state.data) {
              var line = state.data[i]
              if( !line.hidden ) {
                data.push(line);
              }
            }

            if( !state.plot ) {
              state.plot = $.plot(state.placeholder, data, state.options);
              state.placeholder.find('.legend table').each(function () {
                $(this).css('background-color', '#fff');
                $(this).css('opacity', 0.85);

              });
              if( state.options.legend.hidden ) {
                state.placeholder.find('.legend').each(function () {
                  $(this).css('display', 'none');
                });
              }


            } else {
              state.plot.setData(data);
              state.plot.setupGrid();
              state.plot.draw();
            }
            updateLegend(el);
          }
        });
      }
    });
  }

  return {
    
    percent_formater: function (val, axis) {
      if( axis == null ) {
        axis = { tickDecimals: 2 }
      }
      return val.toFixed(axis.tickDecimals) + "%"
    },
    memory_formater : function (val, axis) {
      if( axis == null ) {
        axis = { tickDecimals: 2 }
      }
      if (val > (1024*1024*1024))
        return (val / (1024*1024*1024)).toFixed(axis.tickDecimals) + " gb";
      else if (val > (1024*1024))
        return (val / (1024*1024)).toFixed(axis.tickDecimals) + " mb";
      else if (val > 1024)
        return (val / 1024).toFixed(axis.tickDecimals) + " kb";
      else
        return val + " b";
    },

    update:update_charts,
    setup:initialize_charts
  }
});