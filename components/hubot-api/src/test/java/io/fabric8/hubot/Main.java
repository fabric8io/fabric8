/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.hubot;

import io.fabric8.utils.Systems;

/**
 * A simple program to send a notification to hubot
 */
public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: room messages");
            return;
        }
        String room = args[0];
        StringBuilder buffer = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (i > 1) {
                buffer.append(" ");
            }
            buffer.append(arg);
        }
        String message = buffer.toString();

        try {
            String hubotUrl = Systems.getServiceHostAndPort(HubotNotifier.HUBOT_SERVICE_NAME, "hubot-webhook.fabric8.local", "80");
            String username = Systems.getEnvVarOrSystemProperty("HUBOT_USERNAME", "");
            String password = Systems.getEnvVarOrSystemProperty("HUBOT_PASSWORD", "");
            String roomExpression = Systems.getEnvVarOrSystemProperty("HUBOT_BUILD_ROOM", HubotNotifier.DEFAULT_ROOM_EXPRESSION);

            System.out.println("Logging into hubot web hook with user " + username + " at URL: " + hubotUrl);

            HubotNotifier notifier = new HubotNotifier(hubotUrl, username, password, roomExpression);
            notifier.notifyRoom(room, message);
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }
}
