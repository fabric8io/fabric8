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
  "controllers/application"
  "controllers/agents_page"
  "controllers/welcome_page"
  "controllers/agent_detail_page"
  "controllers/activemq_page"
  "controllers/signin_page"
  "controllers/profiles_page"
  "controllers/profile_details_page"
  "controllers/camel_page"
  "controllers/osgi_page"
  "controllers/users_page"
  "controllers/connection_detail_page"
  "controllers/network_connector_detail_page"
  "controllers/queue_detail_page"
  "controllers/topic_detail_page"
  "controllers/user_settings_page"
  "controllers/cloud_page"
  "controllers/patches_page"
  "controllers/registry_page"
], (app, Application) ->
  $ ->
    application = new Application()
    app.model.set poll_interval: window.get_local_storage('poll_interval', 1000);
    app.router.navigate("/containers", true)  unless Backbone.history.start({})
