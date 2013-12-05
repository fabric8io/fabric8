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
  "views/jade"
  "views/charts",
], (jade, charts) ->

  FON.TemplateController.extend
    elements:
      "#heap_charts": "heap_charts"
    initialize: ->
      @template = jade["agents_page/memory_overview.jade"]
      @bind "render", =>
        
        calc_percent = (data, id1, id2) ->
          used = data[id1]
          max = data[id2]
          if used and max
            rc = []
            for i of used.data
              rc.push [ used.data[i][0], 100.0 * used.data[i][1] / max.data[i][1] ]
            return rc
          false

        @chart1 = @make("div", {style:"width:300px;height:150px"})
        @heap_charts.append(@chart1)
        @chart2 = @make("div", {style:"width:300px;height:150px"})
        @heap_charts.append(@chart2)
        url = "#{@options.url}/extensions/monitor"
        charts.setup @chart1,
          formater:charts.memory_formater
          view:"5min"
          data:[
            ref:"#{url}/fetch" 
            set:"jvm-default"
            sources:[
              {stack:"b", id:"java.lang:name=PS Old Gen,type=MemoryPool@Usage@used"}
              {stack:"b", id:"java.lang:name=PS Survivor Space,type=MemoryPool@Usage@used"}
              {stack:"b", id:"java.lang:name=PS Eden Space,type=MemoryPool@Usage@used"}
              {stack:"a", id:"java.lang:name=CMS Old Gen,type=MemoryPool@Usage@used"}
              {stack:"a", id:"java.lang:name=Par Survivor Space,type=MemoryPool@Usage@used"}
              {stack:"a", id:"java.lang:name=Par Eden Space,type=MemoryPool@Usage@used"}
              {id:"java.lang:type=Memory@HeapMemoryUsage@committed"}
            ]
          ]
          
        charts.setup @chart2,
          formater:charts.percent_formater
          view:"5min"
          max:100
          min:0
          data:[
            ref:"#{url}/fetch" 
            set:"jvm-default"
            sources: [
              {hidden:"true", id:"java.lang:name=PS Old Gen,type=MemoryPool@Usage@used"}
              {hidden:"true", id:"java.lang:name=PS Old Gen,type=MemoryPool@Usage@max"}
              {hidden:"true", id:"java.lang:name=PS Survivor Space,type=MemoryPool@Usage@used"}
              {hidden:"true", id:"java.lang:name=PS Survivor Space,type=MemoryPool@Usage@max"}
              {hidden:"true", id:"java.lang:name=PS Eden Space,type=MemoryPool@Usage@used"}
              {hidden:"true", id:"java.lang:name=PS Eden Space,type=MemoryPool@Usage@max"}
              {hidden:"true", id:"java.lang:name=CMS Old Gen,type=MemoryPool@Usage@used"}
              {hidden:"true", id:"java.lang:name=CMS Old Gen,type=MemoryPool@Usage@max"}
              {hidden:"true", id:"java.lang:name=Par Survivor Space,type=MemoryPool@Usage@used"}
              {hidden:"true", id:"java.lang:name=Par Survivor Space,type=MemoryPool@Usage@max"}
              {hidden:"true", id:"java.lang:name=Par Eden Space,type=MemoryPool@Usage@used"}
              {hidden:"true", id:"java.lang:name=Par Eden Space,type=MemoryPool@Usage@max"}
            ]
          ]
          
          calcs:[
            { id:"PS Old Gen %", label:"PS Old Gen Space Usage", apply: (data) -> 
              calc_percent(data, "java.lang:name=PS Old Gen,type=MemoryPool@Usage@used", "java.lang:name=PS Old Gen,type=MemoryPool@Usage@max"); }
            { id:"PS Survivor Space %", label:"PS Survivor Space Usage", apply: (data) -> 
              calc_percent(data, "java.lang:name=PS Survivor Space,type=MemoryPool@Usage@used", "java.lang:name=PS Survivor Space,type=MemoryPool@Usage@max"); }
            { id:"PS Eden Space %", label:"PS Eden Space Usage", apply: (data) -> 
              return calc_percent(data, "java.lang:name=PS Eden Space,type=MemoryPool@Usage@used", "java.lang:name=PS Eden Space,type=MemoryPool@Usage@max"); }
            { id:"CMS Old Gen %", label:"CMS Old Gen Space Usage", apply: (data) -> 
              calc_percent(data, "java.lang:name=CMS Old Gen,type=MemoryPool@Usage@used", "java.lang:name=PS Old Gen,type=MemoryPool@Usage@max"); }
            { id:"Par Survivor Space %", label:"Par Survivor Space Usage", apply: (data) -> 
              return calc_percent(data, "java.lang:name=Par Survivor Space,type=MemoryPool@Usage@used", "java.lang:name=Par Survivor Space,type=MemoryPool@Usage@max"); }
            { id:"Par Eden Space %", label:"Par Eden Space Usage", apply: (data) -> 
              calc_percent(data, "java.lang:name=Par Eden Space,type=MemoryPool@Usage@used", "java.lang:name=Par Eden Space,type=MemoryPool@Usage@max"); }
          ]
        @poll()
        
    poll: ->
      charts.update(@chart1)
      charts.update(@chart2)
