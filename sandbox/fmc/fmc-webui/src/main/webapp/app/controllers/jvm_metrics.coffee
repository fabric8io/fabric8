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
], (app, jade) ->

  class JVMMetrics extends FON.TemplateController
    template: jade["agents_page/jvm_metrics.jade"]
    template_data: -> @model.toJSON()

    initialize: ->
      super
      @zero = new Number(0).toFixed(2)
      @model.set
        percent: @zero
      @model.bind "change", => @render()

    poll: ->
      @model.fetch
        op: "update"
        success: (model, resp) =>
          @cpu_after = model.get "os_cpu_time"
          @nano_after = new Date().getTime()
          os_processors = model.get "os_processors"

          if @cpu_before && @nano_before && os_processors

            diff = @cpu_after - @cpu_before
            time_between_samples = (@nano_after - @nano_before) * 1000 * 1000

            frac = diff / time_between_samples
            frac = frac / os_processors

            percent = frac * 100

            if !percent || percent < 0 || percent == Infinity
              percent = @zero
            if percent > 100
              percent = 100
            @model.set
              percent: new Number(percent).toFixed(2)
          else
            @model.set
              percent: @zero

          @nano_before = @nano_after
          @cpu_before = @cpu_after

  JVMMetrics

