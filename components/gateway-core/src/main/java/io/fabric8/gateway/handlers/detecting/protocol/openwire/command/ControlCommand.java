/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.gateway.handlers.detecting.protocol.openwire.command;

import org.fusesource.hawtbuf.UTF8Buffer;

/**
 * Used to start and stop transports as well as terminating clients.
 * 
 * @openwire:marshaller code="14"
 * 
 * @version $Revision: 1.1 $
 */
public class ControlCommand extends BaseCommand {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.CONTROL_COMMAND;

    private UTF8Buffer command;

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    /**
     * @openwire:property version=1
     */
    public UTF8Buffer getCommand() {
        return command;
    }

    public void setCommand(UTF8Buffer command) {
        this.command = command;
    }

}
